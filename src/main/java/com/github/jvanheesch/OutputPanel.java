package com.github.jvanheesch;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Scroll behavior: https://stackoverflow.com/a/21067431/1939921
 */
public class OutputPanel extends Panel {
    private static final long serialVersionUID = -5846263527089267321L;

    public OutputPanel(String id) {
        super(id);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String markupId = this.getMarkupId();

        String js = "Wicket.Event.subscribe('/websocket/message', function (jqEvent, message) {\n" +
                "    var pre = $('#" + markupId + "').find('pre'); " +
                "    var pre_dom = pre[0];" +
                "    var isScrolledToBottom = pre_dom.scrollHeight - pre_dom.clientHeight <= pre_dom.scrollTop + 1;\n" +
                "    pre.append(message);\n" +
                "    if (isScrolledToBottom) {\n" +
                "        pre_dom.scrollTop = pre_dom.scrollHeight - pre_dom.clientHeight;\n" +
                "    }\n" +
                "});";

        response.render(OnLoadHeaderItem.forScript(js));
    }
}

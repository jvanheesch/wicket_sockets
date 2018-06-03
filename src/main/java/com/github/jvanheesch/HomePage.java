package com.github.jvanheesch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.WebSocketSettings.Holder;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.message.TextMessage;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * https://github.com/martin-g/blogs/blob/master/wicket6-websocket-broadcast/src/main/java/com/wicketinaction/FeedPage.java
 * java9 -> dont forget: export JAVA_HOME=/usr/lib/jvm/java-9-oracle
 */
public class HomePage extends WebPage {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogManager.getLogger();
    private static final String HOME_DIRECTORY = "/home/" + System.getProperty("user.name") + "/";

    @Override
    protected void onInitialize() {
        super.onInitialize();

        this.add(new OutputPanel("outputPanel"));

        this.add(new WebSocketBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConnect(ConnectedMessage message) {
                super.onConnect(message);

                LOG.info("Client connected");
            }

            @Override
            protected void onMessage(WebSocketRequestHandler handler, TextMessage message) {
                LOG.warn("Received message {}", message.getText());
            }

            @Override
            public void onException(Component component, RuntimeException exception) {
                LOG.warn("Got exception", exception);
            }
        });

        this.add(new AjaxLink<Void>("link") {
            private static final long serialVersionUID = -761452522002059407L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

                Application application = Application.get();

                Runnable ls = () -> {
                    LOG.info("starting runnable");
                    try (
                            InputStream is = new ProcessBuilder()
                                    .command("ls", "-a")
                                    .directory(new File(HOME_DIRECTORY))
                                    .start()
                                    .getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is))
                    ) {
                        WebSocketSettings webSocketSettings = Holder.get(application);
                        Stream.generate(Computable.supplier(reader::readLine))
                                .takeWhile(Objects::nonNull)
                                .map(HomePage::escapeHTML)
                                .map(line -> line + "<br/>")
                                .forEach(line -> {
                                    WebSocketPushBroadcaster broadcaster = new WebSocketPushBroadcaster(webSocketSettings.getConnectionRegistry());
                                    broadcaster.broadcastAll(application, new SimpleWebSocketPushMessage(line));
                                    // simulate slower process
                                    Executable.executeSilently(() -> Thread.sleep(100));
                                });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                };
                executor.schedule(ls, 2, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * https://stackoverflow.com/a/25228492/1939921
     */
    private static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Override
    public void onEvent(IEvent<?> event) {
        if (event.getPayload() instanceof WebSocketPushPayload) {
            WebSocketPushPayload wsEvent = (WebSocketPushPayload) event
                    .getPayload();
            wsEvent.getHandler().push(wsEvent.getMessage().toString());
        }
    }

    private static class SimpleWebSocketPushMessage implements IWebSocketPushMessage {
        private static final long serialVersionUID = -4772070540229885919L;

        private final String message;

        private SimpleWebSocketPushMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return this.message;
        }
    }
}

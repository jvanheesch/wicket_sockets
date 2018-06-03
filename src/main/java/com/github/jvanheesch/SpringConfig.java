package com.github.jvanheesch;

import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.ws.javax.JavaxWebSocketFilter;
import org.apache.wicket.protocol.ws.javax.WicketServerEndpointConfig;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * http://apache-wicket.1842946.n4.nabble.com/javax-websockets-in-Spring-Boot-project-404-error-td4678080.html
 */
@Configuration
public class SpringConfig {

    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setName("WicketApplication");
        registration.setFilter(new JavaxWebSocketFilter());
        registration.addInitParameter("applicationClassName", WicketApplication.class.getName());
        registration.addInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public WicketServerEndpointConfig wicketServerEndpointConfig() {
        return new WicketServerEndpointConfig();
    }
}

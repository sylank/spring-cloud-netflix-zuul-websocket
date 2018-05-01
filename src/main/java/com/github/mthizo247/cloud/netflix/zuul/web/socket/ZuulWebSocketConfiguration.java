/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mthizo247.cloud.netflix.zuul.web.socket;

import com.github.mthizo247.cloud.netflix.zuul.web.authentication.BasicAuthPrincipalHeadersCallback;
import com.github.mthizo247.cloud.netflix.zuul.web.authentication.CompositeHeadersCallback;
import com.github.mthizo247.cloud.netflix.zuul.web.authentication.LoginCookieHeadersCallback;
import com.github.mthizo247.cloud.netflix.zuul.web.authentication.OAuth2BearerPrincipalHeadersCallback;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.block.FilterManager;
import com.github.mthizo247.cloud.netflix.zuul.web.proxytarget.*;
import com.github.mthizo247.cloud.netflix.zuul.web.redirect.ProxyRedirectFilter;
import com.github.mthizo247.cloud.netflix.zuul.web.util.DefaultErrorAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Zuul reverse proxy web socket configuration
 *
 * @author Ronald Mthombeni
 * @author Salman Noor
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(WebSocketHandler.class)
@ConditionalOnProperty(prefix = "zuul.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZuulWebSocketProperties.class)
@AutoConfigureAfter(DelegatingWebSocketMessageBrokerConfiguration.class)
public class ZuulWebSocketConfiguration extends AbstractWebSocketMessageBrokerConfigurer
        implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    ZuulWebSocketProperties zuulWebSocketProperties;
    @Autowired
    SimpMessagingTemplate messagingTemplate;
    @Autowired
    ZuulProperties zuulProperties;
    @Autowired
    @Qualifier("compositeProxyTargetResolver")
    ProxyTargetResolver proxyTargetResolver;
    @Autowired
    ProxyWebSocketErrorHandler proxyWebSocketErrorHandler;
    @Autowired
    WebSocketStompClient stompClient;
    @Autowired
    @Qualifier("compositeHeadersCallback")
    WebSocketHttpHeadersCallback webSocketHttpHeadersCallback;

    @Autowired
    FilterManager filterManager;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        boolean wsEnabled = false;
        for (Map.Entry<String, ZuulWebSocketProperties.WsBrokerage> entry : zuulWebSocketProperties
                .getBrokerages().entrySet()) {
            ZuulWebSocketProperties.WsBrokerage wsBrokerage = entry.getValue();
            if (wsBrokerage.isEnabled()) {
                this.addStompEndpoint(registry, wsBrokerage.getEndPoints());
                wsEnabled = true;
            }
        }

        if (!wsEnabled)
            this.addStompEndpoint(registry, UUID.randomUUID().toString());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // prefix for subscribe
        for (Map.Entry<String, ZuulWebSocketProperties.WsBrokerage> entry : zuulWebSocketProperties
                .getBrokerages().entrySet()) {
            ZuulWebSocketProperties.WsBrokerage wsBrokerage = entry.getValue();
            if (wsBrokerage.isEnabled()) {
                config.enableSimpleBroker(
                        mergeBrokersWithApplicationDestinationPrefixes(wsBrokerage));
                // prefix for send
                config.setApplicationDestinationPrefixes(
                        wsBrokerage.getDestinationPrefixes());
            }
        }
    }

    private SockJsServiceRegistration addStompEndpoint(StompEndpointRegistry registry, String... endpoint) {
        return registry.addEndpoint(endpoint)
                // bypasses spring web security
                .setAllowedOrigins("*").withSockJS();
    }

    private String[] mergeBrokersWithApplicationDestinationPrefixes(
            ZuulWebSocketProperties.WsBrokerage wsBrokerage) {
        List<String> brokers = new ArrayList<>(Arrays.asList(wsBrokerage.getBrokers()));

        for (String adp : wsBrokerage.getDestinationPrefixes()) {
            if (!brokers.contains(adp)) {
                brokers.add(adp);
            }
        }

        return brokers.toArray(new String[brokers.size()]);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler handler) {
                ProxyWebSocketHandler proxyWebSocketHandler = new ProxyWebSocketHandler(
                        handler, stompClient, webSocketHttpHeadersCallback,
                        messagingTemplate,
                        proxyTargetResolver,
                        zuulWebSocketProperties,
                        filterManager);
                proxyWebSocketHandler.errorHandler(proxyWebSocketErrorHandler);
                return proxyWebSocketHandler;
            }
        });
    }

    @Bean
    @Primary
    public WebSocketHttpHeadersCallback compositeHeadersCallback(final List<WebSocketHttpHeadersCallback> headersCallbacks) {
        return new CompositeHeadersCallback(headersCallbacks);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    public WebSocketHttpHeadersCallback basicAuthPrincipalHeadersCallback() {
        return new BasicAuthPrincipalHeadersCallback();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.OAuth2Authentication")
    public WebSocketHttpHeadersCallback oauth2BearerPrincipalHeadersCallback() {
        return new OAuth2BearerPrincipalHeadersCallback();
    }

    @Bean
    public WebSocketHttpHeadersCallback loginCookieHeadersCallback() {
        return new LoginCookieHeadersCallback();
    }

    @Bean
    public ProxyTargetResolver urlProxyTargetResolver(
            final ZuulProperties zuulProperties) {
        return new UrlProxyTargetResolver(zuulProperties);
    }

    @Bean
    public ProxyTargetResolver discoveryProxyTargetResolver(
            final ZuulProperties zuulProperties, final DiscoveryClient discoveryClient) {
        return new EurekaProxyTargetResolver(discoveryClient, zuulProperties);
    }

    @Bean
    public ProxyTargetResolver loadBalancedProxyTargetResolver(
            final ZuulProperties zuulProperties, final LoadBalancerClient loadBalancerClient) {
        return new LoadBalancedProxyTargetResolver(loadBalancerClient, zuulProperties);
    }

    @Bean
    @Primary
    public ProxyTargetResolver compositeProxyTargetResolver(final List<ProxyTargetResolver> resolvers) {
        return new CompositeProxyTargetResolver(resolvers);
    }

    @Bean
    @ConditionalOnMissingBean(WebSocketStompClient.class)
    public WebSocketStompClient stompClient(WebSocketClient webSocketClient, MessageConverter messageConverter,
                                            @Qualifier("proxyStompClientTaskScheduler") TaskScheduler taskScheduler) {
        int bufferSizeLimit = 1024 * 1024 * 8;

        WebSocketStompClient client = new WebSocketStompClient(webSocketClient);
        client.setInboundMessageSizeLimit(bufferSizeLimit);
        client.setMessageConverter(messageConverter);
        client.setTaskScheduler(taskScheduler);
        client.setDefaultHeartbeat(new long[]{0, 0});
        return client;
    }

    @Bean
    @ConditionalOnMissingBean(WebSocketClient.class)
    public WebSocketClient webSocketClient() {
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(webSocketClient));
        return new SockJsClient(transports);
    }

    @Bean
    @Qualifier("proxyStompClientTaskScheduler")
    public TaskScheduler stompClientTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("ProxyStompClient-");
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        return scheduler;
    }

    @Bean
    public ProxyWebSocketErrorHandler reconnectErrorHandler() {
        return new ReconnectErrorHandler(new DefaultErrorAnalyzer());
    }

    @Bean
    @Primary
    public ProxyWebSocketErrorHandler compositeErrorHandler(final List<ProxyWebSocketErrorHandler> errorHandlers) {
        return new CompositeErrorHandler(errorHandlers);
    }

    @Bean
    public ProxyRedirectFilter proxyRedirectFilter(RouteLocator routeLocator) {
        return new ProxyRedirectFilter(routeLocator);
    }

    @PostConstruct
    public void init() {
        ignorePattern("**/websocket");
        ignorePattern("**/info");
    }

    private void ignorePattern(String ignoredPattern) {
        for (String pattern : zuulProperties.getIgnoredPatterns()) {
            if (pattern.toLowerCase().contains(ignoredPattern))
                return;
        }

        zuulProperties.getIgnoredPatterns().add(ignoredPattern);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init();
    }
}

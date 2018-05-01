package com.github.mthizo247.cloud.netflix.zuul.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FilterConfig {
    @Autowired
    private List<WebsocketFilter> websocketFilters;

    @Bean
    public FilterManager filterManager() {
        return new DefaultFilterManager(websocketFilters);
    }
}

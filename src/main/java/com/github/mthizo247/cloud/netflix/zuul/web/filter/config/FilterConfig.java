package com.github.mthizo247.cloud.netflix.zuul.web.filter.config;

import com.github.mthizo247.cloud.netflix.zuul.web.filter.DefaultFilterManager;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.FilterManager;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.WebSocketFilter;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.context.ContextHelper;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.context.WebSocketContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class FilterConfig {
    @Autowired
    private List<WebSocketFilter> webSocketFilters;

    @Bean
    public FilterManager filterManager() {
        return new DefaultFilterManager(webSocketFilters);
    }

    @Bean
    public ContextHelper webSocketContextHelper() {
        return new WebSocketContextHelper();
    }
}

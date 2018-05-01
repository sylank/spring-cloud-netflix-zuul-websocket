package com.github.mthizo247.cloud.netflix.zuul.web.filter.config;

import com.github.mthizo247.cloud.netflix.zuul.web.filter.block.DefaultFilterManager;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.block.FilterManager;
import com.github.mthizo247.cloud.netflix.zuul.web.filter.block.WebSocketFilter;
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
}

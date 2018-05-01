package com.github.mthizo247.cloud.netflix.zuul.web.filter.block;

import com.github.mthizo247.cloud.netflix.zuul.web.filter.exception.FilterException;

import java.util.Collections;
import java.util.List;

public class DefaultFilterManager implements FilterManager{
    private final List<WebSocketFilter> webSocketFilters;

    public DefaultFilterManager(List<WebSocketFilter> webSocketFilters) {
        this.webSocketFilters = webSocketFilters;

        Collections.sort(webSocketFilters);
    }

    @Override
    public void evaluate(final String destination, final Object msg) throws FilterException {
        for (WebSocketFilter webSocketFilter : webSocketFilters) {
            webSocketFilter.doFilterProcess(destination, msg);
        }
    }
}

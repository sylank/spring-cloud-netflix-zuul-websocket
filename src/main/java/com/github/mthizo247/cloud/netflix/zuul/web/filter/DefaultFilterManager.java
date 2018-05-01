package com.github.mthizo247.cloud.netflix.zuul.web.filter;

import java.util.Collections;
import java.util.List;

public class DefaultFilterManager implements FilterManager{
    private final List<WebsocketFilter> websocketFilters;

    public DefaultFilterManager(List<WebsocketFilter> websocketFilters) {
        this.websocketFilters = websocketFilters;

        Collections.sort(websocketFilters);
    }

    @Override
    public void evaluate(final String destination, final Object msg) throws FilterException {
        for (WebsocketFilter websocketFilter : websocketFilters) {
            websocketFilter.doFilterProcess(destination, msg);
        }
    }
}

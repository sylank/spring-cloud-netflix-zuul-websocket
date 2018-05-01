package com.github.mthizo247.cloud.netflix.zuul.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebsocketFilter implements Comparable<WebsocketFilter>{
    private final Logger logger = LoggerFactory.getLogger(WebsocketFilter.class);

    public abstract void doFilter(final String destination, final Object msg) throws FilterException;

    public abstract int getOrder();

    public boolean isBlockingFilter() {
        return true;
    }

    public void doFilterProcess(final String destination, final Object msg) throws FilterException {
        try {
            doFilter(destination,msg);
        } catch (RuntimeException ex) {
            if (isBlockingFilter()) {
                throw ex;
            } else {
                logger.debug(this.getClass().getCanonicalName()+" filter throws exception but it is a non blocking filter.");
            }
        }
    }

    @Override
    public int compareTo(WebsocketFilter o) {
        return this.getOrder()-o.getOrder();
    }
}

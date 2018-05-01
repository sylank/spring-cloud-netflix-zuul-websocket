package com.github.mthizo247.cloud.netflix.zuul.web.filter;

public interface FilterManager {
    public void evaluate(final String destination, final Object msg) throws FilterException;
}

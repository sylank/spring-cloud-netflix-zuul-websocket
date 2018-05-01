package com.github.mthizo247.cloud.netflix.zuul.web.filter.block;

import com.github.mthizo247.cloud.netflix.zuul.web.filter.exception.FilterException;

public interface FilterManager {
    public void evaluate(final String destination, final Object msg) throws FilterException;
}

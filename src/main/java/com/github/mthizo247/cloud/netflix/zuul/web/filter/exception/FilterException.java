package com.github.mthizo247.cloud.netflix.zuul.web.filter.exception;

public class FilterException extends RuntimeException {
    public FilterException(final String msg) {
        super(msg);
    }

    public FilterException() {
        super();
    }
}

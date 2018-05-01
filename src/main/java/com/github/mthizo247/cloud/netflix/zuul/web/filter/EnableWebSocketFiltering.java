package com.github.mthizo247.cloud.netflix.zuul.web.filter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FilterConfig.class)
public @interface EnableWebSocketFiltering {
}

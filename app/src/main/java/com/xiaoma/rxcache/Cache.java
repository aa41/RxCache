package com.xiaoma.rxcache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * author: mxc
 * date: 2018/6/22.
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface Cache {
    long time();
    String[] bindParams();
}

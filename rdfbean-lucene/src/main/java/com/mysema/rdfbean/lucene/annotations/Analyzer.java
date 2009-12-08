/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Define an Analyzer for a given entity, method, field or Field The order of
 * precedence is as such: - @Field - field / method - entity - default
 * 
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface Analyzer {
    Class<?> impl() default void.class;

    String definition() default "";
}

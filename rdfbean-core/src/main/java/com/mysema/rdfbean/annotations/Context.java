/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Target context (URI) for given class or the classes of an annotated package.
 * <p>
 * Many RDF persistence solutions store quadruples containing the
 * context/model/source of the statement instead of simple triples. Queries can
 * thus be targeted to a specific context.
 * 
 * @author sasa
 * 
 */
@Documented
@Target({ TYPE, PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Context {

    /**
     * @return
     */
    String value();

}

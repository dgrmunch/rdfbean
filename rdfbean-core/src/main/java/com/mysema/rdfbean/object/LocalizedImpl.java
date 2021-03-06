/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.object;

import java.lang.annotation.Annotation;

import com.mysema.rdfbean.annotations.Localized;

/**
 * @author tiwe
 * 
 */
@SuppressWarnings("all")
public class LocalizedImpl implements Localized {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Localized.class;
    }

}

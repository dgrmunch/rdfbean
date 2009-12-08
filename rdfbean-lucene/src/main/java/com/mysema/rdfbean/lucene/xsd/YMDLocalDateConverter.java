/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene.xsd;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.mysema.rdfbean.xsd.Converter;

/**
 * YMDLocalDateConverter provides
 *
 * @author tiwe
 * @version $Id$
 */
public enum YMDLocalDateConverter implements Converter<LocalDate>{
    
    YEAR("yyyy"),

    MONTH("yyyyMM"),

    DAY("yyyyMMdd");

    private final DateTimeFormatter formatter;
    
    YMDLocalDateConverter(String pattern) {
        this.formatter = DateTimeFormat.forPattern(pattern);
    }
    
    @Override
    public LocalDate fromString(String str) {
        return formatter.parseDateTime(str).toLocalDate();
    }

    @Override
    public String toString(LocalDate object) {
        return formatter.print(object);
    }

}

/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene.annotations;

/**
 * Defines the term vector storing strategy
 * 
 * @author John Griffin
 */
public enum TermVector {
    /**
     * Store term vectors.
     */
    YES,
    /**
     * Do not store term vectors.
     */
    NO,
    /**
     * Store the term vector + Token offset information
     */
    WITH_OFFSETS,
    /**
     * Store the term vector + token position information
     */
    WITH_POSITIONS,
    /**
     * Store the term vector + Token position and offset information
     */
    WITH_POSITION_OFFSETS
}

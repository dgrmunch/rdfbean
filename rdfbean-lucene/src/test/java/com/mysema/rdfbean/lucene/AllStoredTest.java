/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;

import org.compass.core.Property.Index;
import org.compass.core.Property.Store;
import org.junit.Test;

import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.object.DefaultConfiguration;


/**
 * LuceneConfigurationTest provides
 *
 * @author tiwe
 * @version $Id$
 */
public class AllStoredTest extends AbstractConfigurationTest{
    
    @Test
    public void AllStored(){        
        configuration.setCoreConfiguration(new DefaultConfiguration(AllStored.class));        
        configuration.initialize();    
        
        for (String prop : Arrays.asList("title", "description", "text")){
            PropertyConfig config = configuration.getPropertyConfig(new UID(TEST.NS, prop), 
                    Collections.singleton(new UID(TEST.NS, "AllStored")));
            assertNotNull(config);
            assertEquals(Store.YES, config.getStore());
            assertEquals(Index.NO, config.getIndex());
            assertEquals(1.0f, config.getBoost(), 0.0);
            assertFalse(config.isAllIndexed());
            assertFalse(config.isTextIndexed());
            
        }       
         
    }

//    @ClassMapping(ns=TEST.NS)
//    @Searchable
//    public static class Explicit{
//        
//        @Predicate
//        @SearchablePredicate
//        String account;
//        
//    }
    
    @ClassMapping(ns=TEST.NS)
    @Searchable(storeAll=true)
    public static class AllStored{
        
        @Predicate
        @SearchablePredicate(index=Index.NO)
        String text;
        
        @Predicate
        String title, description;
    }
    

    
}

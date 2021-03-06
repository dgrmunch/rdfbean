/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Id;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;

/**
 * @author sasa
 * 
 */
public class ConstructorParameters2Test {

    @ClassMapping
    public static final class Child {
        @Id
        final ID id;

        @Predicate
        final Parent parent;

        public Child(ID id, Parent parent) {
            this.id = id;
            this.parent = parent;
        }
    }

    @ClassMapping
    public static final class Parent {
        @Id
        ID id;
    }

    @Test
    public void ConstructorInjection() {
        Session session = SessionUtil.openSession(Child.class, Parent.class);
        Parent parent = new Parent();
        Child child = new Child(new BID(), parent);
        session.saveAll(parent, child);
        session.flush();
        session.clear();

        Child child2 = session.get(Child.class, child.id);
        assertNotNull(child2);
        assertEquals(child.id, child2.id);
        assertEquals(child.parent.id, child2.parent.id);
    }

}

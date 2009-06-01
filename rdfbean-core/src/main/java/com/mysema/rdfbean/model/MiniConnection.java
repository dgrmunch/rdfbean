/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.model;

import java.io.IOException;
import java.util.Set;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.object.BeanQuery;
import com.mysema.rdfbean.object.RDFBeanTransaction;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SimpleBeanQuery;

/**
 * @author sasa
 *
 */
public class MiniConnection implements RDFConnection {
    
    private final MiniRepository repository;

    public MiniConnection(MiniRepository repository) {
        this.repository = repository;
    }

    @Override
    public BID createBNode() {
        return new BID();
    }

    @Override
    public BeanQuery createQuery(Session session) {
        return new SimpleBeanQuery(session);
    }

    @Override
    public CloseableIterator<STMT> findStatements(ID subject, UID predicate,
            NODE object, UID context, boolean includeInferred) {
        return repository.findStatements(subject, predicate, object, context);
    }

    @Override
    public void update(Set<STMT> removedStatements, Set<STMT> addedStatements) {
        repository.removeStatement(removedStatements.toArray(new STMT[removedStatements.size()]));
        repository.add(addedStatements.toArray(new STMT[addedStatements.size()]));
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public RDFBeanTransaction beginTransaction(Session session,
            boolean readOnly, int txTimeout, int isolationLevel) {
        throw new UnsupportedOperationException("createTransaction");
    }
    

}

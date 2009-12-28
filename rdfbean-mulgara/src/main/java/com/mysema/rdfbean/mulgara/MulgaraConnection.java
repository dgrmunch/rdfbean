/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.mulgara;

import static com.mysema.rdfbean.mulgara.Constants.EMPTY_GRAPH;
import static com.mysema.rdfbean.mulgara.Constants.O_VAR;
import static com.mysema.rdfbean.mulgara.Constants.P_VAR;
import static com.mysema.rdfbean.mulgara.Constants.S_VAR;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.Triple;
import org.mulgara.client.jrdf.GraphElementBuilder;
import org.mulgara.connection.Connection;
import org.mulgara.query.*;
import org.mulgara.query.operation.Deletion;
import org.mulgara.query.operation.Insertion;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.object.BeanQuery;
import com.mysema.rdfbean.object.RDFBeanTransaction;
import com.mysema.rdfbean.object.Session;
import com.mysema.rdfbean.object.SimpleBeanQuery;
import com.mysema.util.SetMap;

/**
 * MulgaraConnection provides
 *
 * @author tiwe
 * @version $Id$
 */
public class MulgaraConnection implements RDFConnection{
    
    private static final Logger logger = LoggerFactory.getLogger(MulgaraConnection.class);
    
    private final Connection connection;
    
    private final GraphElementFactory elementFactory;
    
    private final MulgaraDialect dialect;

    private MulgaraTransaction localTxn;

    private boolean readonlyTnx = false;
    
    public MulgaraConnection(Connection connection) {        
        try {
            this.connection = Assert.notNull(connection);
            this.elementFactory = new GraphElementBuilder();
            this.dialect = new MulgaraDialect(elementFactory);
        } catch (GraphException e) {
            String error = "Caught " + e.getClass().getName();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    public void cleanUpAfterCommit(){
        localTxn = null;
        readonlyTnx = false;
    }
    
    public void cleanUpAfterRollback(){
        localTxn = null;
        readonlyTnx = false;
        close();
    }
    
    @Override
    public RDFBeanTransaction beginTransaction(boolean readOnly, int txTimeout,
            int isolationLevel) {
        localTxn = new MulgaraTransaction(this, connection, readOnly, txTimeout, isolationLevel);
        readonlyTnx = readOnly;
        return localTxn;
    }

    @Override
    public void clear() {
    }

    @Override
    public BID createBNode() {
        return dialect.getBID(dialect.createBNode());
    }

    @Override
    public BeanQuery createQuery(Session session) {
        return new SimpleBeanQuery(session);
    }

    @Override
    public <Q> Q createQuery(Session session, Class<Q> queryType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CloseableIterator<STMT> findStatements(ID subject, UID predicate,
            NODE object, UID context, boolean includeInferred) {
        List<SelectElement> variableList = new ArrayList<SelectElement>();        
        if (subject == null){
            variableList.add(S_VAR);
        }
        if (predicate == null){
            variableList.add(P_VAR);
        }
        if (object == null){
            variableList.add(O_VAR);
        }
        URI contextURI = context != null ? URI.create(context.getId()) : EMPTY_GRAPH;
        GraphExpression graphExpression = new GraphResource(contextURI);        
        // FIXME!
        ConstraintExpression constraintExpression = new ConstraintImpl(
                (ConstraintElement)(subject != null ? dialect.getResource(subject) : S_VAR),
                (ConstraintElement)(predicate != null ? dialect.getURI(predicate) : P_VAR),
                (ConstraintElement)(object != null ? dialect.getNode(object) : O_VAR),
                new URIReferenceImpl(contextURI));
        Query query = new Query(variableList,
                graphExpression, 
                constraintExpression, 
                null, /* constraintHaving */
                Collections.<Order>emptyList(), /* order */ 
                null, /* limit */ 
                0,  /* offset */
                new UnconstrainedAnswer());
        
        try {
            Answer answer = connection.execute(query);
            return new MulgaraResultIterator(dialect, answer, subject, predicate, object, context);
        } catch (QueryException e) {
            String error = "Caught " + e.getClass().getName();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        } catch (TuplesException e) {
            String error = "Caught " + e.getClass().getName();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    @Override
    public void update(Set<STMT> removedStatements, Set<STMT> addedStatements) {        
        if (!readonlyTnx){
            try {
                // group by context
                SetMap<URI,Triple> removed = new SetMap<URI,Triple>();
                SetMap<URI,Triple> added = new SetMap<URI,Triple>();
                for (STMT stmt : removedStatements){
                    URI context = stmt.getContext() != null ? URI.create(stmt.getContext().getId()) : EMPTY_GRAPH;
                    removed.put(context, convert(stmt));
                }
                for (STMT stmt : addedStatements){
                    URI context = stmt.getContext() != null ? URI.create(stmt.getContext().getId()) : EMPTY_GRAPH;
                    added.put(context, convert(stmt));
                }
                
                // apply deletions
                for (Map.Entry<URI, Set<Triple>> entry : removed.entrySet()){
                    connection.execute(new Deletion(entry.getKey(), entry.getValue()));
                }                
                // apply insertions
                for (Map.Entry<URI, Set<Triple>> entry : added.entrySet()){
                    connection.execute(new Insertion(entry.getKey(), entry.getValue()));
                }
            } catch (Exception e) {
                String error = "Caught " + e.getClass().getName();
                logger.error(error, e);
                throw new RuntimeException(error, e);
            }
        }
    }
    
    private Triple convert(STMT stmt){
        return dialect.createStatement(stmt.getSubject(), stmt.getPredicate(),stmt.getObject(),stmt.getContext());
    }

    @Override
    public void close(){
        try {
            connection.close();
        } catch (QueryException e) {
            String error = "Caught " + e.getClass().getName();
            logger.error(error, e);
            throw new RuntimeException(error, e);
        }
        
    }

}
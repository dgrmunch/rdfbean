/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.rdb;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nullable;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLQueryImpl;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLMergeClause;
import com.mysema.query.types.path.PEntity;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IdSequence;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.rdb.support.DateTimeType;
import com.mysema.rdfbean.rdb.support.LocalDateType;
import com.mysema.rdfbean.rdb.support.LocalTimeType;
import com.mysema.rdfbean.xsd.ConverterRegistry;

/**
 * RDBContext provides
 *
 * @author tiwe
 * @version $Id$
 */
public class RDBContext implements Closeable{
    
    private final ConverterRegistry converterRegistry;
    
    private final RDBOntology ontology;
    
    private final Connection connection;
    
    private final IdFactory idFactory;
    
    private final IdSequence idSequence;
    
    private final BidiMap<Locale,Integer> langCache;
    
    private final BidiMap<NODE,Long> nodeCache;
    
    private final BidiMap<NODE,Long> localCache = new DualHashBidiMap<NODE,Long>();
        
    private final Configuration configuration;
    
    public RDBContext(
            ConverterRegistry converterRegistry,
            RDBOntology ontology,
            IdFactory idFactory,
            BidiMap<NODE,Long> nodeCache,  
            BidiMap<Locale,Integer> langCache,
            IdSequence idSequence,
            Connection connection, 
            SQLTemplates templates) {
        this.converterRegistry = converterRegistry;
        this.ontology = ontology;
        this.idFactory = idFactory;
        this.idSequence = idSequence;
        this.nodeCache = nodeCache;
        this.langCache = langCache;
        this.connection = connection;
        this.configuration = new Configuration(templates);
        configuration.register(new DateTimeType());
        configuration.register(new LocalDateType());
        configuration.register(new LocalTimeType());
    }
    
    public RDFBeanTransaction beginTransaction(boolean readOnly, int txTimeout, int isolationLevel) {
        try {
            connection.setAutoCommit(false);
            connection.setReadOnly(readOnly);
            if (isolationLevel != -1){
                connection.setTransactionIsolation(isolationLevel);    
            }else{
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }            
            return new RDBTransaction(connection);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }        
    }

    public void clear() {
        localCache.clear();
    }
    
    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
    
    public SQLDeleteClause createDelete(PEntity<?> entity){
        return new SQLDeleteClause(connection, configuration, entity);
    }
    
    public SQLInsertClause createInsert(PEntity<?> entity){
        return new SQLInsertClause(connection, configuration, entity);
    }
    
    public SQLMergeClause createMerge(PEntity<?> entity){
        return new SQLMergeClause(connection, configuration, entity);
    }

    public SQLQuery createQuery(){
        return new SQLQueryImpl(connection, configuration);
    }

    @Nullable
    public Locale getLang(int id) {
        return langCache.getKey(id);
    }

    public Integer getLangId(Locale locale) {
        Integer id = langCache.get(locale);
        if (id == null){
            id = idFactory.getId(locale);
            langCache.put(locale, id);
        }
        return id;
    }

    public long getNextLocalId() {
        return idSequence.getNextId();
    }

    @Nullable
    public NODE getNode(long id, Transformer<Long,NODE> t) {
        if (nodeCache.containsValue(id)){
            return nodeCache.getKey(id);
        }else if (localCache.containsValue(id)){
            return localCache.getKey(id);
        }else{
            NODE node = t.transform(id);
            localCache.put(node, id);
            return node;
        }
    }

    public Long getNodeId(NODE node) {
        if (nodeCache.containsKey(node)){
            return nodeCache.get(node);
        }else if (localCache.containsKey(node)){    
            return localCache.get(node);
        }else{
            Long id = idFactory.getId(node);
            localCache.put(node, id);
            return id;
        }        
    }

    public Collection<NODE> getNodes() {
        return nodeCache.keySet();
    }

    public java.sql.Date toDate(LIT literal){
        return converterRegistry.fromString(literal.getValue(), java.sql.Date.class);
    }
    
    public java.sql.Timestamp toTimestamp(LIT literal){
        return converterRegistry.fromString(literal.getValue(), java.sql.Timestamp.class);
    }
    
    public ID getID(String lexical, boolean resource){
        return resource ? new UID(lexical) : new BID(lexical);
    }

    public RDBOntology getOntology() {
        return ontology;
    }

    public Long getId(Object constant) {
        // TODO : cache
        UID type = converterRegistry.getDatatype(constant.getClass());
        String lexical = converterRegistry.toString(constant);
        return getNodeId(new LIT(lexical, type));
    }
    
    
}

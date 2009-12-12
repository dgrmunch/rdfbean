/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene;

import static com.mysema.rdfbean.lucene.Constants.ALL_FIELD_NAME;
import static com.mysema.rdfbean.lucene.Constants.CONTEXT_FIELD_NAME;
import static com.mysema.rdfbean.lucene.Constants.CONTEXT_NULL;
import static com.mysema.rdfbean.lucene.Constants.ID_FIELD_NAME;
import static com.mysema.rdfbean.lucene.Constants.TEXT_FIELD_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.lucene.index.CorruptIndexException;
import org.compass.core.Compass;
import org.compass.core.CompassHits;
import org.compass.core.CompassQueryBuilder;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.Property.Index;
import org.compass.core.Property.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.RDF;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.object.BeanQuery;
import com.mysema.rdfbean.object.RDFBeanTransaction;
import com.mysema.rdfbean.object.Session;
import com.mysema.util.ListMap;

/**
 * LuceneConnection provides
 *
 * @author tiwe
 * @version $Id$
 */
public abstract class AbstractLuceneConnection implements RDFConnection{
    
    private static final List<String> INTERNAL_FIELDS = Arrays.asList(
            "alias",
            "$/uid",
            ALL_FIELD_NAME, 
            CONTEXT_FIELD_NAME,
            ID_FIELD_NAME,
            TEXT_FIELD_NAME);
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractLuceneConnection.class);
    
    protected final Compass compass;
    
    protected final CompassSession compassSession;
    
    protected final LuceneConfiguration conf;
    
    @Nullable
    private LuceneTransaction localTxn = null;
    
    private boolean readonlyTnx = false;
        
    public AbstractLuceneConnection(LuceneConfiguration configuration, CompassSession session) {
        this.conf = Assert.notNull(configuration);        
        this.compassSession = Assert.notNull(session);
        this.compass = conf.getCompass();
    }

    public void addStatement(Resource resource, boolean component, STMT stmt, List<ID> subjectTypes) {        
        String objectValue = conf.getConverter().toString(stmt.getObject());        
        PropertyConfig propertyConfig = conf.getPropertyConfig(stmt.getPredicate(), subjectTypes);
        
        if (propertyConfig != null){            
            if (propertyConfig.getStore() != Store.NO || propertyConfig.getIndex() != Index.NO){
                String predicateField = conf.getConverter().uidToShortString(stmt.getPredicate());
                if (component){
                    predicateField = conf.getConverter().toString(stmt.getSubject()) + " " + predicateField; 
                }                
                Property property = compass.getResourceFactory().createProperty(predicateField, objectValue, 
                        propertyConfig.getStore(), propertyConfig.getIndex());
                resource.addProperty(property);
            }     
            
            if (propertyConfig.isAllIndexed()){
                resource.addProperty(ALL_FIELD_NAME, objectValue);
            }            
            
            if (propertyConfig.isTextIndexed()){
                resource.addProperty(TEXT_FIELD_NAME, stmt.getObject().getValue());
            }            
        }
        
    }
    
    @Override
    public RDFBeanTransaction beginTransaction(boolean readOnly,
            int txTimeout, int isolationLevel) {
        CompassTransaction tx = compassSession.beginTransaction();
        readonlyTnx = readOnly;
        localTxn = new LuceneTransaction(this, tx);        
        return localTxn;
    }
    
    public void cleanUpAfterCommit(){
        localTxn = null;
        readonlyTnx = false;
    }
    
    public void cleanUpAfterRollback(){
        localTxn = null;
        readonlyTnx = false;
        // NOTE : LuceneConnection is closed after rollback
        close();
    }

    @Override
    public void clear() {
        compassSession.evictAll();        
    }
    
    @Override
    public void close()  {
        compassSession.close();
    }
    
    @Override
    public BID createBNode() {
        return new BID();
    }

    @Override
    public BeanQuery createQuery(Session session) {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <Q> Q createQuery(Session session, Class<Q> queryType) {
        if (queryType.equals(LuceneQuery.class)){
            return (Q)new LuceneQuery(conf, session, compassSession);
        }else if (queryType.equals(CompassQueryBuilder.class)){    
            return (Q) compass.queryBuilder();
        }else{
            throw new IllegalArgumentException("Unsupported query type : " + queryType.getSimpleName());
        }
    }
    
    private Resource createResource(){
        return compass.getResourceFactory().createResource("resource");
    }
    
    protected List<STMT> findStatements(Resource resource, ID subject, UID predicate, NODE object, UID context){
        // TODO : how to handle embedded properties of components?!?
        List<STMT> stmts = new ArrayList<STMT>();
        ID sub = subject;
        UID pre = predicate;
        NODE obj = object;                    
        if (sub == null){
            sub = (ID) conf.getConverter().fromString(resource.getId());
        }                    
        if (pre != null){
            if (obj != null){
                stmts.add(new STMT(sub, pre, obj));
            }else{
                for (Property property : resource.getProperties(getPredicateField(pre))){
                    obj = conf.getConverter().fromString(property.getStringValue());
                    stmts.add(new STMT(sub, pre, obj));
                }
            }
            
        }else if (obj != null){
            String objString = conf.getConverter().toString(obj);
            for (Property property : resource.getProperties()){
                if (isPredicateProperty(property.getName())  
                        && objString.equals(property.getStringValue())){
                    pre = conf.getConverter().uidFromShortString(property.getName());
                    stmts.add(new STMT(sub, pre, obj));
                }
            }
            
        }else{
            for (Property property : resource.getProperties()){
                if (isPredicateProperty(property.getName())){ 
                    pre = conf.getConverter().uidFromShortString(property.getName());
                    obj = conf.getConverter().fromString(property.getStringValue());
                    stmts.add(new STMT(sub, pre, obj));
                }    
            }            
        }
        return stmts;
    }
    
    private String getPredicateField(UID predicate){
        return conf.getConverter().uidToShortString(predicate);
    }

    private Resource getResource(String field, Object value) throws IOException {
        CompassHits hits = compassSession.queryBuilder().term(field, value).hits();
        return hits.getLength() > 0 ? hits.resource(0) : null;
    }
          
    private boolean isPredicateProperty(String fieldName){
        return !INTERNAL_FIELDS.contains(fieldName) && !fieldName.contains(" ");
    }
    
    private void update(ListMap<ID,ID> types, ListMap<ID, STMT> rsAdded, ListMap<ID, STMT> rsRemoved,
            Set<ID> resources) throws IOException, CorruptIndexException {

        for (ID resource : resources) {
            String id = conf.getConverter().toString(resource);
            Resource luceneResource = getResource(ID_FIELD_NAME, id);

            if (luceneResource == null) {
                luceneResource = createResource();
                luceneResource.addProperty(ID_FIELD_NAME, id);
                HashSet<ID> contextsToAdd = new HashSet<ID>();
                List<STMT> list = rsAdded.get(resource);
                if (list != null){
                    for (STMT s : list) {
                        List<ID> subjectTypes = types.get(s.getSubject());
                        if (subjectTypes == null){
                            subjectTypes = Collections.emptyList();
                        }
                        addStatement(luceneResource, !s.getSubject().equals(resource), s, subjectTypes);
                        if (s.getContext() != null){
                            contextsToAdd.add(s.getContext());    
                        }                        
                    }
                }
                    
                if (conf.isContextsStored()){
                    if (contextsToAdd.isEmpty()){
                        luceneResource.addProperty(CONTEXT_FIELD_NAME, CONTEXT_NULL);
                    }else{
                        for (ID c : contextsToAdd) {
                            luceneResource.addProperty(CONTEXT_FIELD_NAME, conf.getConverter().toString(c));
                        }    
                    }                                                           
                }
                
                compassSession.save(luceneResource);

                if (rsRemoved.containsKey(resource)){
                    logger.warn(rsRemoved.get(resource).size() +
                        " statements are marked to be removed that should not be in the store," + 
                        " for resource " + resource + ". Nothing done.");
                }
                    
            } else {
                Resource newResource = createResource();

                ListMap<String, String> removedOfResource = null;
                List<STMT> removedStatements = rsRemoved.get(resource);
                if (removedStatements != null) {
                    removedOfResource = new ListMap<String, String>();
                    for (STMT r : removedStatements) {
                        removedOfResource.put(r.getPredicate().getValue(), 
                                conf.getConverter().toString(r.getObject()));
                    }
                }

                for (Property oldProperty : luceneResource.getProperties()) {
                    if (removedOfResource != null) {
                        List<String> objectsRemoved = removedOfResource.get(oldProperty.getName());
                        if ((objectsRemoved != null) && (objectsRemoved.contains(oldProperty.getStringValue()))) {
                            continue;
                        }

                    }
                    newResource.addProperty(oldProperty);
                }

                List<STMT> addedToResource = rsAdded.get(resource);
                if (addedToResource != null) {
                    HashSet<ID> contextsToAdd = new HashSet<ID>();
                    for (STMT s : addedToResource) {
                        List<ID> subjectTypes = types.get(s.getSubject());
                        if (subjectTypes == null){
                            List<STMT> typeStmts = findStatements(luceneResource, s.getSubject(), RDF.type, null, null); 
                            subjectTypes = new ArrayList<ID>(typeStmts.size());
                            for (STMT stmt : typeStmts){
                                subjectTypes.add((ID) stmt.getObject());
                            }
                        }
                        addStatement(newResource, !s.getSubject().equals(resource), s, subjectTypes);
                        if (s.getContext() != null){
                            contextsToAdd.add(s.getContext());    
                        }                        
                    }

                    if (conf.isContextsStored()){
                        if (contextsToAdd.isEmpty()){
                            newResource.addProperty(CONTEXT_FIELD_NAME, CONTEXT_NULL);
                        }else{
                            for (ID c : contextsToAdd) {
                                newResource.addProperty(CONTEXT_FIELD_NAME, conf.getConverter().toString(c));
                            }
                        }
                            
                    }                    
                }

                compassSession.save(newResource);
            }
        }        
    }
        
    @Override
    public void update(Set<STMT> removed, Set<STMT> added) {
        if (!readonlyTnx){
            ListMap<ID, STMT> rsAdded = new ListMap<ID, STMT>();
            ListMap<ID, STMT> rsRemoved = new ListMap<ID, STMT>();
            ListMap<ID, ID> types = new ListMap<ID, ID>();
            HashSet<ID> resources = new HashSet<ID>();

            Map<ID,ID> componentToHost;
            if (!conf.getComponentProperties().isEmpty()){
                componentToHost = new HashMap<ID,ID>();
                for (Set<STMT> stmts : Arrays.asList(added, removed)){
                    for (STMT s :stmts){
                        if (conf.getComponentProperties().contains(s.getPredicate())){
                            componentToHost.put((ID) s.getObject(), s.getSubject());
                        }
                    }   
                }
            }else{
                componentToHost = Collections.emptyMap();
            }
            
            // populate rsAdded and rsRemoved
            for (Set<STMT> stmts : Arrays.asList(added ,removed)){
                ListMap<ID,STMT> target = stmts == added ? rsAdded : rsRemoved;
                for (STMT s : stmts){
                    if (componentToHost.containsKey(s.getSubject())){
                        target.put(componentToHost.get(s.getSubject()), s);    
                    }else{
                        target.put(s.getSubject(), s);
                    }                
                    resources.add(s.getSubject());
                    if (s.getPredicate().equals(RDF.type)){
                        types.put(s.getSubject(), (ID) s.getObject());
                    }
                }                                  
            }
            
            try {
                update(types, rsAdded, rsRemoved, resources);
            } catch (CorruptIndexException e) {
                String error = "Caught " + e.getClass().getName();
                logger.error(error, e);
                throw new RuntimeException(error, e);
            } catch (IOException e) {
                String error = "Caught " + e.getClass().getName();
                logger.error(error, e);
                throw new RuntimeException(error, e);
            }
        }                
    }


}

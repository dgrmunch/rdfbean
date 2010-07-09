package com.mysema.rdfbean.rdb;

import static com.mysema.rdfbean.rdb.QLanguage.language;
import static com.mysema.rdfbean.rdb.QStatement.statement;
import static com.mysema.rdfbean.rdb.QSymbol.symbol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mysema.commons.l10n.support.LocaleUtil;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLMergeClause;
import com.mysema.query.types.EConstructor;
import com.mysema.query.types.Expr;
import com.mysema.rdfbean.model.*;
import com.mysema.rdfbean.object.Session;

/**
 * RDBConnection provides
 *
 * @author tiwe
 * @version $Id$
 */
public class RDBConnection implements RDFConnection{
   
    public static final QSymbol sub = new QSymbol("subject");
    
    public static final QSymbol pre = new QSymbol("predicate");
    
    public static final QSymbol obj = new QSymbol("object");
    
    public static final QSymbol con = new QSymbol("context");
    
    private final RDBContext context;
    
    public RDBConnection(RDBContext context) {
        this.context = context;
    }

    public Integer addLang(Locale locale){
        Integer langId = getId(locale);
        SQLMergeClause merge = context.createMerge(language);
        merge.keys(language.id);
        merge.set(language.id, langId);
        merge.set(language.text, LocaleUtil.toLang(locale));
        merge.execute();
        return langId;
    }

    public Long addNode(NODE node) {
        Long nodeId = getId(node);
        SQLMergeClause merge = context.createMerge(symbol);
        merge.keys(symbol.id);
        merge.set(symbol.id, nodeId);
        merge.set(symbol.resource, node.isResource());
        merge.set(symbol.lexical, node.getValue());
        if (node.isLiteral()){
            LIT literal = node.asLiteral();
            merge.set(symbol.datatype, getId(literal.getDatatype()));    
            merge.set(symbol.lang, getLangId(literal.getLang()));
            if (context.isIntegerType(literal.getDatatype())){
                merge.set(symbol.integer, Long.valueOf(literal.getValue()));
            }
            if (context.isDecimalType(literal.getDatatype())){
                merge.set(symbol.floating, Double.valueOf(literal.getValue()));
            }
            if (context.isDateType(literal.getDatatype())){
                merge.set(symbol.datetime, context.toDate(literal));                
            }
            if (context.isDateTimeType(literal.getDatatype())){
                merge.set(symbol.datetime, context.toTimestamp(literal));
            }
        }
        merge.execute();
        return nodeId;
    }

    public void addStatement(STMT stmt) {
        // TODO : fix merge clause behaviour in Querydsl SQL
        SQLQuery query = context.createQuery();
        query.from(statement);
        if (stmt.getContext() != null){
            query.where(statement.model.eq(getId(stmt.getContext())));    
        }else{
            query.where(statement.model.isNull());
        }        
        query.where(statement.subject.eq(getId(stmt.getSubject())));
        query.where(statement.predicate.eq(getId(stmt.getPredicate())));
        query.where(statement.object.eq(getId(stmt.getObject())));
        if (query.count() > 0l){
            return;
        }
        
        SQLMergeClause merge = context.createMerge(statement);
        merge.keys(statement.model, statement.subject, statement.predicate, statement.object);
        if (stmt.getContext() != null){
            merge.set(statement.model, getId(stmt.getContext()));    
        }else{
            merge.set(statement.model, null);
        }        
        merge.set(statement.subject, getId(stmt.getSubject()));
        merge.set(statement.predicate, getId(stmt.getPredicate()));
        merge.set(statement.object, getId(stmt.getObject()));
        merge.execute();
    }

    @Override
    public RDFBeanTransaction beginTransaction(boolean readOnly, int txTimeout, int isolationLevel) {
        return context.beginTransaction(readOnly, txTimeout, isolationLevel);
    }

    @Override
    public void clear() {
        context.clear();        
    }
    
    @Override
    public void close() throws IOException {
        context.close();
    }

    @Override
    public BID createBNode() {
        return new BID("_"+UUID.randomUUID());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D, Q> Q createQuery(Session session, QueryLanguage<D, Q> queryLanguage, D definition) {
        if (queryLanguage.equals(QueryLanguage.QUERYDSL)){
            return (Q)new RDBQuery(context,session);            
        }else{
            throw new UnsupportedQueryLanguageException(queryLanguage);
        }
    }
    
    public List<STMT> find(ID subject, UID predicate, NODE object, UID model, boolean includeInferred) {
        return IteratorAdapter.asList(findStatements(subject, predicate, object, model, includeInferred));
    }

    @Override
    @SuppressWarnings("serial")
    public CloseableIterator<STMT> findStatements(
            final ID subject, 
            final UID predicate, 
            final NODE object, 
            final UID model, boolean includeInferred) {
        SQLQuery query = this.context.createQuery();
        query.from(statement);
        List<Expr<?>> exprs = new ArrayList<Expr<?>>();
        if (subject != null){
            query.where(statement.subject.eq(getId(subject)));
        }else{
            query.innerJoin(statement.subjectFk, sub);
            exprs.add(sub.lexical);
        }
        if (predicate != null){
            query.where(statement.predicate.eq(getId(predicate)));
        }else{
            exprs.add(statement.predicate);
        }
        if (object != null){
            query.where(statement.object.eq(getId(object)));
        }else{
            query.innerJoin(statement.objectFk, obj);
            exprs.add(obj.resource);
            exprs.add(obj.lexical);
            exprs.add(obj.datatype);
            exprs.add(obj.lang);
        }
        if (model != null){
            query.where(statement.model.eq(getId(model)));
        }else{
            exprs.add(statement.model);
        }
        
        EConstructor<STMT> stmt = new EConstructor<STMT>(STMT.class, new Class[0],exprs.toArray(new Expr[exprs.size()])){
            @Override
            public STMT newInstance(Object... args) {
                ID s = subject;
                UID p = predicate;
                NODE o = object;
                UID m = model;
                int counter = 0;
                if (s == null){
                    s = getNode(true, (String)args[counter++], null, null).asResource();
                }
                if (p == null){
                    p = getNode((Long) args[counter++]).asURI();
                }                
                if (o == null){
                    o = getNode((Boolean)args[counter++], (String)args[counter++], (Long)args[counter++], (Integer)args[counter++]);
                }
                if (m == null && args[counter] != null && !args[counter].equals(Long.valueOf(0l))){
                    m = getNode((Long) args[counter]).asURI();
                }
                return new STMT(s, p, o, m);
            }
      
        };        
        return query.iterate(stmt);
    }
    
    @Nullable
    private Integer getId(@Nullable Locale locale){
        if (locale == null){
            return null;
        }else{
            return context.getLangId(locale);
        }
    }
    
    private Long getId(NODE node) {
        return context.getNodeId(node);
    }

    @Nullable
    private Integer getLangId(@Nullable Locale lang) {
        if (lang == null){
            return null;
        }else{
            return addLang(lang);    
        }
    }

    private Locale getLocale(int id){
        return context.getLang(id);
    }
    
    @Override
    public long getNextLocalId() {
        return context.getNextLocalId();
    }

    private NODE getNode(boolean res, String lex, Long datatype, Integer lang){
        if (res){
            if (lex.startsWith("_")){
                return new BID(lex);
            }else{
                return new UID(lex);
            }
        }else{
            if (lang != null && !lang.equals(Integer.valueOf(0))){
                return new LIT(lex, getLocale(lang));
            }else if (datatype != null && !datatype.equals(Long.valueOf(0l))){
                return new LIT(lex, getNode(datatype).asURI());
            }else{
                return new LIT(lex);
            }
        }
    }
    
    private NODE getNode(long id) {
        NODE node = context.getNode(id);
        if (node == null){
            SQLQuery query = context.createQuery();
            query.from(symbol);
            query.where(symbol.id.eq(id));
            Object[] result = query.uniqueResult(symbol.resource, symbol.lexical, symbol.datatype, symbol.lang);
            if (result != null){
                return getNode((Boolean)result[0], (String)result[1], (Long)result[2], (Integer)result[3]);
            }else{
                throw new IllegalArgumentException("Found no node for id " + id);
            }
            
        }
        return node;
    }
    
    public void removeStatement(STMT stmt) {
        SQLDeleteClause delete = context.createDelete(statement);
        if (stmt.getContext() == null){
            delete.where(statement.model.isNull());
        }else{
            delete.where(statement.model.eq(getId(stmt.getContext())));
        }
        delete.where(statement.subject.eq(getId(stmt.getSubject())));
        delete.where(statement.predicate.eq(getId(stmt.getPredicate())));
        delete.where(statement.object.eq(getId(stmt.getObject())));
        delete.execute();
    }

    @Override
    public void update(Set<STMT> removedStatements, Set<STMT> addedStatements) {
        // remove
        Set<NODE> oldNodes = new HashSet<NODE>();
        for (STMT stmt : removedStatements){
            if (stmt.getContext() != null){
                oldNodes.add(stmt.getContext());
            }
            oldNodes.add(stmt.getSubject());
            oldNodes.add(stmt.getPredicate());
            oldNodes.add(stmt.getObject());
            removeStatement(stmt);            
        }
        
        // insert
        Set<NODE> newNodes = new HashSet<NODE>();
        Set<UID> newDatatypes = new HashSet<UID>();
        for (STMT stmt : addedStatements){
            if (stmt.getContext() != null){
                newNodes.add(stmt.getContext());
            }
            newNodes.add(stmt.getSubject());
            newNodes.add(stmt.getPredicate());
            newNodes.add(stmt.getObject());           
            if (stmt.getObject().isLiteral()){
                LIT lit = stmt.getObject().asLiteral();
                if (lit.getDatatype() != null){
                    newDatatypes.add(lit.getDatatype());
                }
            }
        }
        
        // insert datatypes
        newDatatypes.removeAll(context.getNodes());
        for (UID uid : newDatatypes){
            addNode(uid);
        }
        
        // insert nodes
        newNodes.removeAll(oldNodes);
        newNodes.removeAll(context.getNodes());        
        for (NODE node : newNodes){
            addNode(node);
        }
        
        // insert stmts
        for (STMT stmt : addedStatements){
            addStatement(stmt);
        }
        
    }

}
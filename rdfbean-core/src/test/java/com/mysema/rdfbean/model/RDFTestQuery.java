package com.mysema.rdfbean.model;

import java.util.Map;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.EmptyCloseableIterator;
import com.mysema.query.types.Expression;

public class RDFTestQuery extends RDFQueryImpl {

    public RDFTestQuery() {
        super(null);
    }

    @Override
    public boolean ask() {
        aggregateFilters();
        SPARQLVisitor visitor = new SPARQLVisitor();
        visitor.visit(queryMixin.getMetadata(), QueryLanguage.BOOLEAN);
        System.out.println(visitor.toString());
        return false;
    }

    @Override
    public CloseableIterator<Map<String, NODE>> select(Expression<?>... exprs) {
        aggregateFilters();
        queryMixin.addProjection(exprs);
        SPARQLVisitor visitor = new SPARQLVisitor();
        visitor.visit(queryMixin.getMetadata(), QueryLanguage.TUPLE);
        System.out.println(visitor.toString());
        return new EmptyCloseableIterator<Map<String, NODE>>();
    }

    @Override
    public CloseableIterator<STMT> construct(Block... exprs) {
        aggregateFilters();
        queryMixin.addProjection(exprs);
        SPARQLVisitor visitor = new SPARQLVisitor();
        visitor.visit(queryMixin.getMetadata(), QueryLanguage.GRAPH);
        System.out.println(visitor.toString());
        return new EmptyCloseableIterator<STMT>();
    }

}

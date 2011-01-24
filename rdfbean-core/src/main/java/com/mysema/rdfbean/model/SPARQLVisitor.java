package com.mysema.rdfbean.model;

import javax.annotation.Nullable;

import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.support.SerializerBase;
import com.mysema.query.types.Constant;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.PathType;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.Templates;
import com.mysema.rdfbean.model.io.NTriplesWriter;

/**
 * @author tiwe
 *
 */
public class SPARQLVisitor extends SerializerBase<SPARQLVisitor>{
    
    private static final Templates templates = new Templates(){{
        add(PathType.VARIABLE, "?{0s}");
    }};
    
    @Nullable
    private PatternBlock lastPattern;
    
    public SPARQLVisitor() {
        super(templates);
    }

    @Nullable
    public Void visit(QueryMetadata expr, @Nullable Void context) {
        QueryModifiers mod = expr.getModifiers();
        // select
        if (!expr.getProjection().isEmpty()){
            append("SELECT ");
            if (expr.isDistinct()){
                append("DISTINCT ");
            }
            boolean first = true;
            for (Expression<?> e : expr.getProjection()){
                if (!first){
                    append(" ");
                    
                }
                handle(e);
                first = false;
            }
            append("\n");
        }else{
            append("ASK \n");
        }        
        
        // where
        if (expr.getWhere() != null){
            append("WHERE \n  ");
            if (expr.getWhere() instanceof GroupBlock){
                handle(expr.getWhere());                
            }else{
                append("{ ");
                handle(expr.getWhere());
                append("}");    
            }
            append("\n");                
        }      
        
        // order
        if (!expr.getOrderBy().isEmpty()){
            append("ORDER BY ");
            boolean first = true;            
            for (OrderSpecifier<?> order : expr.getOrderBy()){
                if (!first){
                    append(" ");                    
                }
                if (order.isAscending()){
                    handle(order.getTarget());
                }else{
                    append("DESC(").handle(order.getTarget()).append(")");
                }
                first = false;
            }
            append("\n");
        }
        // TODO : group by
        // TODO : having    
        
        // limit        
        if (mod.getLimit() != null){
            append("LIMIT ").append(mod.getLimit().toString()).append("\n");
        }
        
        // offset
        if (mod.getOffset() != null){
            append("OFFSET ").append(mod.getOffset().toString()).append("\n");
        }
        return null;
    }
    
    @Override
    public Void visit(SubQueryExpression<?> expr, Void context) {
        return visit(expr.getMetadata(), context);
    }


    @Override
    public Void visit(Constant<?> expr, Void context) {
        Object constant = expr.getConstant();
        if (constant instanceof UID){
            append(NTriplesWriter.toString((UID)constant));
        }else if (constant instanceof BID){
            append(NTriplesWriter.toString((BID)constant));
        }else if (constant instanceof LIT){
            append(NTriplesWriter.toString((LIT)constant));
        }else{
            throw new IllegalArgumentException(expr.toString());
        }
        return null;
    }
    
    @Nullable
    public Void visit(UnionBlock expr, @Nullable Void context) {
        boolean first = true;
        for (Block block : expr.getBlocks()){
            if (!first){
                append("UNION ");
            }
            if (block instanceof PatternBlock){
                append("{ ").handle(block).append("} ");
            }else{
                handle(block);
            }
            lastPattern = null;
            first = false;
        }
        return null;
    }
    
    @Nullable
    public Void visit(GroupBlock expr, @Nullable Void context) {
        // TODO : handle context
        if (expr.isOptional()){
            append("OPTIONAL ");
        }
        append("{ ");
        for (Block block : expr.getBlocks()){
            if (lastPattern != null && !(block instanceof PatternBlock)){
                append(".\n  ");
                lastPattern = null;
            }
            handle(block);
        }
        if (!expr.getFilters().isEmpty()){
            if (lastPattern != null){
                append(". ");
            }
            append("\n  FILTER(");
            for (Predicate predicate : expr.getFilters()){
                handle(predicate);
            }
            append(") ");
        }
        append("} ");
        lastPattern = null;
        return null;
    }
    
    @Nullable
    public Void visit(PatternBlock expr, @Nullable Void context) {
        if (lastPattern == null || !lastPattern.getSubject().equals(expr.getSubject())){
            if (lastPattern != null){
                append(".\n  ");
            }
            handle(expr.getSubject()).append(" ");
            handle(expr.getPredicate()).append(" ");
            
        }else if (!lastPattern.getPredicate().equals(expr.getPredicate())){
            append("; ");
            handle(expr.getPredicate()).append(" ");
            
        }else{
            append(", ");
        }
        
        handle(expr.getObject()).append(" ");
        lastPattern = expr;
        return null;
    }
    
}

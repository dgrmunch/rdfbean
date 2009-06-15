/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.sesame.query.functions;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;

import com.mysema.query.types.operation.Operator;
import com.mysema.rdfbean.model.UID;

/**
 * BooleanFunction provides
 * 
 * @author tiwe
 * @version $Id$
 * 
 */
abstract class BooleanFunction extends BaseFunction {
    public BooleanFunction(UID uri, Operator<?>... ops) {
        super(uri, ops);
    }

    @Override
    public final Value evaluate(ValueFactory valueFactory, Value... args)
            throws ValueExprEvaluationException {
        return valueFactory.createLiteral(convert(args));
    }

    protected abstract boolean convert(Value... args);
}
/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.lucene.annotations;

/**
 * Cache mode strategy for <code>FullTextFilterDef</code>s.
 *
 * @see FullTextFilterDef
 * @author Emmanuel Bernard
 */
public enum FilterCacheModeType {
	/**
	 * No filter instance and no result is cached by Hibernate Search.
	 * For every filter call, a new filter instance is created.
	 */
	NONE,

	/**
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * Results are not cached by Hibernate Search.
	 *
	 * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader)

	 */
	INSTANCE_ONLY,

	/**
	 * Both the filter instance and the <code>DocIdSet</code> results are cached.
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * <code>DocIdSet</code> results are cached per <code>IndexReader</code>.
	 *
	 * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader) 
	 */
	INSTANCE_AND_DOCIDSETRESULTS

}

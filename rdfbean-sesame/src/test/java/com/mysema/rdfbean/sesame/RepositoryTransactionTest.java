package com.mysema.rdfbean.sesame;

import java.sql.Connection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.io.Format;
import com.mysema.rdfbean.model.io.RDFSource;

public class RepositoryTransactionTest {

    private static final int TX_TIMEOUT = -1;

    private static final int TX_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    private static SesameRepository repository;

    @BeforeClass
    public static void setUpClass(){
        repository = new MemoryRepository();
        repository.setSources(
                new RDFSource("classpath:/test.ttl", Format.TURTLE, TEST.NS),
                new RDFSource("classpath:/foaf.rdf", Format.RDFXML, FOAF.NS)
            );
        repository.initialize();
    }

    @AfterClass
    public static void tearDownClass(){
        repository.close();
    }

    @Test
    public void Create_and_Commit(){
        RDFConnection connection = repository.openConnection();
        try{
            RDFBeanTransaction tx = connection.beginTransaction(false, TX_TIMEOUT, TX_ISOLATION);
            tx.commit();
        }finally{
            connection.close();
        }
    }

    @Test
    public void Create_and_Rollback(){
        RDFConnection connection = repository.openConnection();
        try{
            RDFBeanTransaction tx = connection.beginTransaction(false, TX_TIMEOUT, TX_ISOLATION);
            tx.rollback();
        }finally{
            connection.close();
        }
    }
}
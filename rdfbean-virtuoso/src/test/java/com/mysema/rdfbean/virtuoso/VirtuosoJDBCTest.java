package com.mysema.rdfbean.virtuoso;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.RDFS;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.XSD;

public class VirtuosoJDBCTest extends AbstractConnectionTest {

    @Test
    public void SPARQL_via_JDBC_Connection() throws SQLException {
        Connection jdbcConn = (connection).getConnection();
        String javaOutput = "sparql\n define output:format '_JAVA_'\n ";
        // tuples
        query(jdbcConn, "sparql select ?s ?p where { ?s ?p ?o } limit 3");
        query(jdbcConn, javaOutput + "select ?s ?p where { ?s ?p ?o } limit 3");

        // triples
        query(jdbcConn, "sparql construct { ?s ?p ?o } where { ?s ?p ?o } limit 3");
        query(jdbcConn, javaOutput + "construct { ?s ?p ?o } where { ?s ?p ?o } limit 3");
        query(jdbcConn, javaOutput + "construct { ?s ?p ?o ; rdf:type ?type } where { ?s ?p ?o ; rdf:type ?type } limit 3");
        query(jdbcConn, "sparql describe ?s where { ?s ?p ?o } limit 3");
        query(jdbcConn, javaOutput + "describe ?s where { ?s ?p ?o } limit 3");

        // ask
        query(jdbcConn, "sparql ask where { ?s ?p ?o }");
        query(jdbcConn, javaOutput + "ask where { ?s ?p ?o }");
    }

    @Test
    @Ignore
    public void Contexts() throws SQLException {
        ID sub = new UID(TEST.NS, "e" + System.currentTimeMillis());
        UID pred = new UID(TEST.NS, "p" + System.currentTimeMillis());
        List<STMT> stmts = Arrays.asList(
                new STMT(sub, pred, sub, context),
                new STMT(sub, pred, pred, context2)
                );
        toBeRemoved = stmts;
        connection.update(null, stmts);

        Connection jdbcConn = (connection).getConnection();
        Statement stmt = jdbcConn.createStatement();
        ResultSet rs = null;
        try {
            Set<UID> found = new HashSet<UID>();
            // rs = stmt.executeQuery("DB.DBA.SPARQL_SELECT_KNOWN_GRAPHS()");
            rs = stmt.executeQuery("sparql select distinct ?g where { graph ?g { ?s ?p ?o } }");
            while (rs.next()) {
                found.add(new UID(rs.getString(1)));
            }
            for (UID uid : found) {
                System.err.println(uid.getId());
            }

            assertTrue(found.contains(context));
            assertTrue(found.contains(context2));

        } finally {
            AbstractQueryImpl.close(stmt, rs);
        }

    }

    @Test
    public void SPARQL_SELECT_with_Resource_binding() throws SQLException {
        Connection jdbcConn = (connection).getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // example on how to bind a resource
            String query = "sparql select ?s ?p ?o where { ?s `iri(??)` ?o } limit 3";
            ps = jdbcConn.prepareStatement(query);
            ps.setString(1, RDFS.label.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getObject(1) + " " + rs.getObject(2));
            }

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Test
    @Ignore
    public void SPARQL_CONSTRUCT_with_Resource_binding() throws SQLException {
        // FIXME
        Connection jdbcConn = (connection).getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // example on how to bind a resource
            String query = "sparql construct { ?s ?p ?o } from named `iri(??)` where { ?s ?p ?o } limit 3";
            ps = jdbcConn.prepareStatement(query);
            ps.setString(1, RDFS.label.getId());

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    @Test
    public void SPARQL_via_JDBC_Connection_with_Literal_binding() throws SQLException {
        Connection jdbcConn = (connection).getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // example on how to bind a typed literal
            String query = "sparql select ?s ?p ?o where { ?s ?p `bif:__rdf_long_from_batch_params(??,??,??)` } limit 3";
            ps = jdbcConn.prepareStatement(query);
            ps.setInt(1, 4);
            ps.setString(2, "xxx");
            ps.setString(3, XSD.stringType.getId());
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getObject(1) + " " + rs.getObject(2));
            }

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
    }

    private void query(Connection jdbcConn, String query) throws SQLException {
        PreparedStatement ps = jdbcConn.prepareStatement(query);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                System.out.println("Results for " + query);

                // columns
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    System.err.println((i + 1) + " : " + rsmd.getColumnName(i + 1));
                }
                System.err.println();

                // data
                while (rs.next()) {
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        Object obj = rs.getObject(i + 1);
                        System.err.println(obj + " " + obj.getClass().getSimpleName());
                    }
                    System.err.println();
                }
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
    }
}

/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.rdfbean.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.sql.DataSource;

import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.io.Resources;
import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.CloseableIterator;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.rdfbean.CORE;
import com.mysema.rdfbean.Namespaces;
import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.model.Format;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IdSequence;
import com.mysema.rdfbean.model.LIT;
import com.mysema.rdfbean.model.NODE;
import com.mysema.rdfbean.model.Nodes;
import com.mysema.rdfbean.model.RDFBeanTransaction;
import com.mysema.rdfbean.model.RDFConnection;
import com.mysema.rdfbean.model.RDFConnectionCallback;
import com.mysema.rdfbean.model.Repository;
import com.mysema.rdfbean.model.RepositoryException;
import com.mysema.rdfbean.model.STMT;
import com.mysema.rdfbean.model.UID;
import com.mysema.rdfbean.model.XSD;
import com.mysema.rdfbean.model.io.RDFSource;
import com.mysema.rdfbean.model.io.RDFWriter;
import com.mysema.rdfbean.model.io.WriterUtils;
import com.mysema.rdfbean.object.Configuration;
import com.mysema.rdfbean.object.MappedClass;
import com.mysema.rdfbean.object.MappedPath;
import com.mysema.rdfbean.object.MappedPredicate;
import com.mysema.rdfbean.object.MappedProperty;
import com.mysema.rdfbean.rdb.support.SesameDialect;
import com.mysema.rdfbean.xsd.ConverterRegistry;
import com.mysema.rdfbean.xsd.ConverterRegistryImpl;

/**
 * RDBRepository is a Repository implementation for the RDB module
 * 
 * @author tiwe
 * @version $Id$
 */
@Immutable
public class RDBRepository implements Repository {

    private static RDFFormat getRioFormat(Format format) {
        switch (format) {
        case N3:
            return RDFFormat.N3;
        case NTRIPLES:
            return RDFFormat.NTRIPLES;
        case RDFA:
            return RDFFormat.RDFA;
        case RDFXML:
            return RDFFormat.RDFXML;
        case TRIG:
            return RDFFormat.TRIG;
        case TURTLE:
            return RDFFormat.TURTLE;
        }
        throw new IllegalArgumentException("Unsupported format : " + format);
    }

    private static final int LOAD_BATCH_SIZE = 1000;

    private final ConverterRegistry converterRegistry = new ConverterRegistryImpl();

    private final IdFactory idFactory = new MD5IdFactory();

    private final BiMap<NODE, Long> nodeCache = HashBiMap.create();

    private final BiMap<Locale, Integer> langCache = HashBiMap.create();

    private final Configuration configuration;

    private final DataSource dataSource;

    private final SQLTemplates templates;

    private final IdSequence idSequence;

    private final RDFSource[] sources;

    public RDBRepository(
            Configuration configuration,
            DataSource dataSource,
            SQLTemplates templates,
            IdSequence idSequence,
            RDFSource... sources) {
        this.configuration = Assert.notNull(configuration, "configuration");
        this.dataSource = Assert.notNull(dataSource, "dataSource");
        this.templates = Assert.notNull(templates, "templates");
        this.idSequence = Assert.notNull(idSequence, "idSequence");
        this.sources = Assert.notNull(sources, "sources");
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public <RT> RT execute(RDFConnectionCallback<RT> operation) {
        RDFConnection connection = openConnection();
        try {
            try {
                RDFBeanTransaction tx = connection.beginTransaction(false, 0,
                        Connection.TRANSACTION_READ_COMMITTED);
                try {
                    RT retVal = operation.doInConnection(connection);
                    tx.commit();
                    return retVal;
                } catch (IOException io) {
                    tx.rollback();
                    throw io;
                }
            } finally {
                connection.close();
            }
        } catch (IOException io) {
            throw new RepositoryException(io);
        }
    }

    @Override
    public void export(Format format, UID context, OutputStream out) {
        export(format, Namespaces.DEFAULT, context, out);
    }

    @Override
    public void export(Format format, Map<String, String> ns2prefix, UID context, OutputStream out) {
        RDFWriter writer = WriterUtils.createWriter(format, out, ns2prefix);
        RDFConnection conn = openConnection();
        try {
            CloseableIterator<STMT> stmts = conn.findStatements(null, null, null, context, false);
            try {
                writer.begin();
                while (stmts.hasNext()) {
                    writer.handle(stmts.next());
                }
                writer.end();
            } finally {
                stmts.close();
            }

        } finally {
            conn.close();
        }
    }

    @Override
    public void load(Format format, InputStream is, @Nullable UID context, boolean replace) {
        ValueFactory valueFactory = new ValueFactoryImpl();
        SesameDialect dialect = new SesameDialect(valueFactory);
        RDBConnection connection = openConnection();
        try {
            if (!replace && context != null) {
                if (!connection.find(null, null, null, context, false).isEmpty()) {
                    return;
                }
            }
            if (context != null && replace) {
                connection.deleteFromContext(context);
            }
            Set<STMT> stmts = new HashSet<STMT>(LOAD_BATCH_SIZE);
            RDFParser parser = Rio.createParser(getRioFormat(format));
            parser.setRDFHandler(createHandler(dialect, connection, stmts, context));
            parser.parse(is, context != null ? context.getValue() : TEST.NS);
            connection.update(Collections.<STMT> emptySet(), stmts);
        } catch (RDFParseException e) {
            throw new RepositoryException(e);
        } catch (RDFHandlerException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } finally {
            connection.close();
        }
    }

    @Override
    public void initialize() {
        try {
            initSchema();
            initTables();

            if (sources.length > 0) {
                RDBConnection connection = openConnection();
                try {
                    ValueFactory valueFactory = new ValueFactoryImpl();
                    SesameDialect dialect = new SesameDialect(valueFactory);
                    for (RDFSource source : sources) {
                        Set<STMT> stmts = new HashSet<STMT>(LOAD_BATCH_SIZE);
                        RDFFormat format = getRioFormat(source.getFormat());
                        RDFParser parser = Rio.createParser(format);
                        UID context = new UID(source.getContext());
                        connection.deleteFromContext(context);
                        parser.setRDFHandler(createHandler(dialect, connection, stmts, context));
                        parser.parse(source.openStream(), source.getContext());
                        connection.update(Collections.<STMT> emptySet(), stmts);
                    }
                } catch (RDFParseException e) {
                    throw new RepositoryException(e);
                } catch (RDFHandlerException e) {
                    throw new RepositoryException(e);
                } finally {
                    connection.close();
                }
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private RDFHandler createHandler(
            final SesameDialect dialect,
            final RDBConnection connection, final Set<STMT> stmts, @Nullable final UID context) {
        return new RDFHandlerBase() {
            @Override
            public void handleStatement(Statement stmt) throws RDFHandlerException {
                ID sub = dialect.getID(stmt.getSubject());
                UID pre = dialect.getUID(stmt.getPredicate());
                NODE obj = dialect.getNODE(stmt.getObject());
                stmts.add(new STMT(sub, pre, obj, context));

                if (stmts.size() == LOAD_BATCH_SIZE) {
                    connection.update(Collections.<STMT> emptySet(), stmts);
                    stmts.clear();
                }
            }
        };
    }

    @SuppressWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    private void initSchema() throws IOException, SQLException {
        Connection conn = dataSource.getConnection();
        try {
            SQLQuery query = new SQLQuery(conn, templates).from(QLanguage.language);
            query.count();
        } catch (Exception e) {
            java.sql.Statement stmt = conn.createStatement();
            try {
                URL res = Thread.currentThread().getContextClassLoader().getResource("h2.sql");
                String sql = Resources.toString(res, Charsets.ISO_8859_1);
                for (String clause : sql.split(";")) {
                    if (!clause.trim().isEmpty()) {
                        stmt.execute(clause.trim());
                    }
                }
            } finally {
                stmt.close();
            }

        } finally {
            conn.close();
        }

    }

    private void initTables() throws IOException {
        RDBConnection conn = openConnection();
        try {

            // init languages
            Set<Locale> locales = new HashSet<Locale>(Arrays.asList(Locale.getAvailableLocales()));
            locales.add(new Locale(""));
            locales.add(new Locale("en"));
            locales.add(new Locale("fi"));
            locales.add(new Locale("sv"));

            conn.addLocales(locales, langCache);

            Set<NODE> nodes = new HashSet<NODE>();

            // ontology resources
            for (MappedClass mappedClass : configuration.getMappedClasses()) {
                // class id
                nodes.add(mappedClass.getUID());

                // enum constants
                if (mappedClass.isEnum()) {
                    for (Object e : mappedClass.getJavaClass().getEnumConstants()) {
                        nodes.add(new UID(mappedClass.getUID().ns(), ((Enum<?>) e).name()));
                    }
                }

                // property predicates
                for (MappedPath path : mappedClass.getProperties()) {
                    MappedProperty<?> property = path.getMappedProperty();
                    if (property.getKeyPredicate() != null) {
                        nodes.add(property.getKeyPredicate());
                    }
                    if (property.getValuePredicate() != null) {
                        nodes.add(property.getValuePredicate());
                    }
                    for (MappedPredicate predicate : path.getPredicatePath()) {
                        nodes.add(predicate.getUID());
                    }
                }
            }

            // common resources
            nodes.add(new UID("default:default"));
            nodes.add(CORE.localId);
            nodes.add(RDB.nullContext);
            nodes.addAll(Nodes.all);

            // common literals
            nodes.add(new LIT(""));
            nodes.add(new LIT("true", XSD.booleanType));
            nodes.add(new LIT("false", XSD.booleanType));

            // dates
            nodes.add(new LIT(converterRegistry.toString(new java.sql.Date(0)), XSD.date));
            nodes.add(new LIT(converterRegistry.toString(new java.util.Date(0)), XSD.dateTime));

            // letters
            for (char c = 'a'; c <= 'z'; c++) {
                String str = String.valueOf(c);
                nodes.add(new LIT(str));
                nodes.add(new LIT(str.toUpperCase(Locale.ENGLISH)));
            }

            // numbers
            for (int i = -128; i < 128; i++) {
                String str = String.valueOf(i);
                nodes.add(new LIT(str));
                nodes.add(new LIT(str, XSD.byteType));
                nodes.add(new LIT(str, XSD.shortType));
                nodes.add(new LIT(str, XSD.intType));
                nodes.add(new LIT(str, XSD.longType));
                nodes.add(new LIT(str, XSD.integerType));
                nodes.add(new LIT(str + ".0", XSD.floatType));
                nodes.add(new LIT(str + ".0", XSD.doubleType));
                nodes.add(new LIT(str + ".0", XSD.decimalType));
            }

            conn.addNodes(nodes, nodeCache);

        } finally {
            conn.close();
        }
    }

    @Override
    public RDBConnection openConnection() {
        try {
            Connection connection = dataSource.getConnection();
            RDBContext context = new RDBContext(
                    converterRegistry,
                    idFactory,
                    nodeCache, langCache,
                    idSequence,
                    connection,
                    templates);
            return new RDBConnection(context);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

}

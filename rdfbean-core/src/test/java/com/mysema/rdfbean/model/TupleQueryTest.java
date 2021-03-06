package com.mysema.rdfbean.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.mysema.commons.lang.CloseableIterator;
import com.mysema.commons.lang.IteratorAdapter;
import com.mysema.query.types.Predicate;
import com.mysema.rdfbean.AbstractConnectionTest;
import com.mysema.rdfbean.TEST;

public class TupleQueryTest extends AbstractConnectionTest {

    private static final QNODE<ID> subject = new QNODE<ID>(ID.class, "s");

    private static final QNODE<UID> predicate = new QNODE<UID>(UID.class, "p");

    private static final QNODE<NODE> object = new QNODE<NODE>(NODE.class, "o");

    private RDFQuery query() {
        return new RDFQueryImpl(connection());
    }

    @Override
    @Before
    public void before() {
        super.before();
        connection().update(null,
                Arrays.asList(
                        new STMT(new BID(), RDFS.label, new LIT("C")),
                        new STMT(new BID(), RDF.type, RDFS.Resource)));
    }

    @Override
    @After
    public void after() {
        connection().remove(null, null, null, null);
        super.after();
    }

    @Test
    public void Pattern() {
        query().where(Blocks.pattern(subject, RDF.type, RDFS.Class)).select(subject);
    }

    @Test
    public void SelectAll() {
        CloseableIterator<Map<String, NODE>> iterator = query().where(Blocks.SPO).selectAll();
        assertTrue(iterator.hasNext());
        try {
            while (iterator.hasNext()) {
                Map<String, NODE> row = iterator.next();
                assertNotNull(row.get("s"));
                assertNotNull(row.get("p"));
                assertNotNull(row.get("o"));
            }
        } finally {
            iterator.close();
        }
    }

    @Test
    public void Pattern_with_Filters() {
        Block pattern = Blocks.pattern(subject, predicate, object);

        List<Predicate> filters = Arrays.<Predicate> asList(
                subject.eq(new UID(TEST.NS)),
                subject.in(RDF.type, RDF.first),
                predicate.eq(RDFS.label),
                subject.ne(new UID(TEST.NS)),
                object.isNull(),
                object.isNotNull(),
                object.stringValue().startsWith("X"),
                object.lit().like("X%"),
                object.lit().matches(".*"),
                object.lit().lt("D"),
                object.lit().gt("B"),
                object.lit().loe("C"),
                object.lit().goe("C"),
                object.lit().eqIgnoreCase("X"),
                object.lit().isEmpty()
                );

        for (Predicate filter : filters) {
            query().where(pattern, filter).selectSingle(subject);
        }
    }

    @Test
    public void Pattern_with_Limit_and_Offset() {
        query().where(Blocks.pattern(subject, RDF.type, RDFS.Class))
                .limit(5)
                .offset(20)
                .select(subject);
    }

    @Test
    public void Group() {
        query().where(
                Blocks.pattern(subject, RDF.type, RDFS.Class),
                Blocks.pattern(subject, predicate, object))
                .select(subject, predicate, object);
    }

    @Test
    public void Union() {
        query().where(
                Blocks.union(
                        Blocks.pattern(subject, RDF.type, RDFS.Class),
                        Blocks.pattern(subject, predicate, object)
                        )).select(subject, predicate, object);
    }

    @Test
    public void Optional() {
        query().where(
                Blocks.pattern(subject, RDF.type, RDFS.Class),
                Blocks.optional(Blocks.pattern(subject, predicate, object)))
                .select(subject, predicate, object);
    }

    @Test
    public void Complex() {
        QID u = new QID("u"), u2 = new QID("u2");
        QLIT label = new QLIT("label");
        UID User = new UID(TEST.NS, "User");

        ID id = new BID(), id2 = new BID(), id3 = new BID();
        connection().update(null,
                Arrays.asList(
                        new STMT(id, RDF.type, User),
                        new STMT(id2, RDF.type, User),
                        new STMT(id3, RDF.type, User),
                        new STMT(id, RDFS.label, new LIT("x")),
                        new STMT(id2, RDFS.label, new LIT("x")),
                        new STMT(id3, RDFS.label, new LIT("y"))));

        CloseableIterator<Map<String, NODE>> iterator =
                query().where(
                        Blocks.pattern(u, RDF.type, User),
                        Blocks.pattern(u2, RDF.type, User),
                        Blocks.pattern(u2, RDFS.label, label),
                        Blocks.pattern(u, RDFS.label, label),
                        u.ne(u2)
                        ).select(u, u2);

        List<Map<String, NODE>> list = IteratorAdapter.asList(iterator);
        assertEquals(2, list.size());
    }

    @Test
    @Ignore
    public void From() {
        UID test = new UID(TEST.NS);
        UID test2 = new UID(TEST.NS, "Res1");
        connection().update(null, Arrays.asList(new STMT(new BID(), RDFS.label, new LIT("C"), test)));

        assertTrue(query().from(test).where(Blocks.pattern(subject, predicate, object)).ask());
        assertTrue(query().from(test, test2).where(Blocks.pattern(subject, predicate, object)).ask());
        assertFalse(query().from(test2).where(Blocks.pattern(subject, predicate, object)).ask());
    }

}

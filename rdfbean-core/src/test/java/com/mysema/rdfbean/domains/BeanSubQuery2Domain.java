package com.mysema.rdfbean.domains;

import static com.mysema.query.types.path.PathMetadataFactory.forVariable;

import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.PEntity;
import com.mysema.query.types.path.PNumber;
import com.mysema.query.types.path.PSimple;
import com.mysema.query.types.path.PString;
import com.mysema.query.types.path.PathInits;
import com.mysema.rdfbean.TEST;
import com.mysema.rdfbean.annotations.ClassMapping;
import com.mysema.rdfbean.annotations.Id;
import com.mysema.rdfbean.annotations.Predicate;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.IDType;

public interface BeanSubQuery2Domain {
    
    @ClassMapping(ns=TEST.NS)
    public static class Revision {
        
        @Id(IDType.RESOURCE)
        public ID id;
        
        @Predicate
        public long svnRevision;
        
        @Predicate
        public long created;
                
        @Predicate
        public Entity revisionOf;

        public long getSvnRevision() {
            return svnRevision;
        }

        public long getCreated() {
            return created;
        }

        public Entity getRevisionOf() {
            return revisionOf;
        }
                                        
    }
    

    @ClassMapping(ns=TEST.NS)
    public static class Entity {
        
        @Id
        public String id;

        @Predicate
        public Document document;
        
        public String getId() {
            return id;
        }

        public Document getDocument() {
            return document;
        }
                                
    }
    
    @ClassMapping(ns=TEST.NS)
    public static class Document {
        
        @Id
        public String id;

        public String getId() {
            return id;
        }
                                
    }
    
    public class QDocument extends PEntity<BeanSubQuery2Domain.Document> {

        private static final long serialVersionUID = 539301614;

        public static final QDocument document = new QDocument("document");

        public final PString id = createString("id");

        public QDocument(String variable) {
            super(BeanSubQuery2Domain.Document.class, forVariable(variable));
        }

        public QDocument(PEntity<? extends BeanSubQuery2Domain.Document> entity) {
            super(entity.getType(),entity.getMetadata());
        }

        public QDocument(PathMetadata<?> metadata) {
            super(BeanSubQuery2Domain.Document.class, metadata);
        }

    }
    
    public class QEntity extends PEntity<BeanSubQuery2Domain.Entity> {

        private static final long serialVersionUID = -1236041098;

        private static final PathInits INITS = PathInits.DIRECT;

        public static final QEntity entity = new QEntity("entity");

        public final QDocument document;

        public final PString id = createString("id");

        public QEntity(String variable) {
            this(BeanSubQuery2Domain.Entity.class, forVariable(variable), INITS);
        }

        public QEntity(PathMetadata<?> metadata) {
            this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
        }

        public QEntity(PathMetadata<?> metadata, PathInits inits) {
            this(BeanSubQuery2Domain.Entity.class, metadata, inits);
        }

        public QEntity(Class<? extends BeanSubQuery2Domain.Entity> type, PathMetadata<?> metadata, PathInits inits) {
            super(type, metadata, inits);
            this.document = inits.isInitialized("document") ? new QDocument(forProperty("document")) : null;
        }

    }

    public class QRevision extends PEntity<BeanSubQuery2Domain.Revision> {

        private static final long serialVersionUID = -583205458;

        private static final PathInits INITS = PathInits.DIRECT;

        public static final QRevision revision = new QRevision("revision");

        public final PNumber<Long> created = createNumber("created", Long.class);

        public final PSimple<com.mysema.rdfbean.model.ID> id = createSimple("id", com.mysema.rdfbean.model.ID.class);

        public final QEntity revisionOf;

        public final PNumber<Long> svnRevision = createNumber("svnRevision", Long.class);

        public QRevision(String variable) {
            this(BeanSubQuery2Domain.Revision.class, forVariable(variable), INITS);
        }

        public QRevision(PathMetadata<?> metadata) {
            this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
        }

        public QRevision(PathMetadata<?> metadata, PathInits inits) {
            this(BeanSubQuery2Domain.Revision.class, metadata, inits);
        }

        public QRevision(Class<? extends BeanSubQuery2Domain.Revision> type, PathMetadata<?> metadata, PathInits inits) {
            super(type, metadata, inits);
            this.revisionOf = inits.isInitialized("revisionOf") ? new QEntity(forProperty("revisionOf"), inits.get("revisionOf")) : null;
        }

    }
    
}

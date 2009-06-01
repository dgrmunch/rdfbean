/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.rdfbean.object.identity;

import java.beans.DefaultPersistenceDelegate;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.mysema.rdfbean.model.BID;
import com.mysema.rdfbean.model.ID;
import com.mysema.rdfbean.model.LID;
import com.mysema.rdfbean.model.UID;
import com.mysema.util.BidirectionalMap;

/**
 * MemoryIdentityService is a memory based implementation of the IdentityService interface
 * 
 * @author sasa
 * 
 */
public class MemoryIdentityService implements IdentityService {
    
    private File cacheFile;
    
    private static final HashMap<String, IdentityService> instances = new HashMap<String, IdentityService>();
    
    static {
        instances.put(null, new MemoryIdentityService());
    }

    private int idCount = 0;
    
    private final BidirectionalMap<IDKey, LID> ids;

    private void persist() throws IOException {
        if (ids.size() != idCount) {
            if (!cacheFile.exists()) {
                System.out.println("CREATING LOCAL ID CACHE: " + cacheFile);
                File parent = cacheFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                cacheFile.createNewFile();
            } else {
                System.out.println("SAVING CHANGES TO LOCAL ID CACHE: " + cacheFile);
            }
            XMLEncoder out = null;
            try {
                out = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(cacheFile)));
                out.setPersistenceDelegate(BidirectionalMap.class, 
                        new DefaultPersistenceDelegate(new String[] { "values", "factory" }));
                out.setPersistenceDelegate(LIDFactory.class, new DefaultPersistenceDelegate(new String[] { "nextId" }));
                out.setPersistenceDelegate(IDKey.class, new DefaultPersistenceDelegate(new String[] { "model", "id" }));
                out.setPersistenceDelegate(UID.class, new DefaultPersistenceDelegate(new String[] { "namespace", "localName" }));
                out.setPersistenceDelegate(BID.class, new DefaultPersistenceDelegate(new String[] { "id" }));
                out.setPersistenceDelegate(LID.class, new DefaultPersistenceDelegate(new String[] { "id" }));
                out.setExceptionListener(new ExceptionListener() {
                    @Override
                    public void exceptionThrown(Exception e) {
                        e.printStackTrace();
                    }});
                out.writeObject(ids);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private MemoryIdentityService(String dirName, String fileName) {
        this(new File(dirName, fileName));
    }

    private MemoryIdentityService() {
        ids = new BidirectionalMap<IDKey, LID>(new LIDFactory());
    }

    @SuppressWarnings("unchecked")
    private MemoryIdentityService(File file) {
        this.cacheFile = file;
        if (cacheFile.exists()) {
            System.out.println("USING LOCAL ID CACHE: " + cacheFile);
            XMLDecoder in = null;
            try {
                in = new XMLDecoder(new BufferedInputStream(new FileInputStream(cacheFile)));
                ids = (BidirectionalMap<IDKey, LID>) in.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            idCount = ids.size();
        } else {
            ids = new BidirectionalMap<IDKey, LID>(new LIDFactory());
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    persist();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public LID getLID(UID uid) {
        return getLID(null, uid);
    }

    @Override
    public synchronized LID getLID(ID model, ID rid) {
        if (rid instanceof UID) {
            return ids.get(new IDKey((UID) rid));
        } else {
            return ids.get(new IDKey(model, (BID) rid));
        }
    }


//    @Override
//    public synchronized UID createID(UID namespace, String localNamePrefix) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public synchronized ID getID(LID lid) {
        IDKey rid = ids.getKey(lid);
        if (rid == null) {
            throw new IllegalArgumentException("No such local id: " + lid);
        } else {
            return rid.getId();
        }
    }

    public static IdentityService instance() {
        return instances.get(null);
    }

    public synchronized static IdentityService instance(String fileName) {
        if (StringUtils.isNotEmpty(fileName)) {
            return instance(new File(fileName).getAbsoluteFile());
        } else {
            return instance();
        }
    }

    public synchronized static IdentityService instance(File file) {
        IdentityService instance = instances.get(file.getAbsoluteFile().getName());
        if (instance == null) {
            instance = new MemoryIdentityService(file);
            instances.put(file.getAbsoluteFile().getName(), instance);
        }
        return instance;
    }

}

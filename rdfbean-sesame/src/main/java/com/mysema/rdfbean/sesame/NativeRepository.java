/**
 * 
 */
package com.mysema.rdfbean.sesame;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 * @author sasa
 *
 */
public class NativeRepository extends AbstractSesameRepository {
    
    private File dataDir;

    @Override
    public Repository createRepository() {
        NativeStore store = new NativeStore(dataDir);
        return new SailRepository(new ForwardChainingRDFSInferencer(store));
    }

    public void setDataDir(File dataDir) {
        this.dataDir = dataDir;
    }
    
    public void setDataDirName(String dataDirName) {
        if (StringUtils.isNotEmpty(dataDirName)) {
            this.dataDir = new File(dataDirName);
        }
    }

}

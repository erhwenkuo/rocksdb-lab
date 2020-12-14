package io.github.erhwenkuo.rocksdb.lab4;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;

// demo to open existing RocksDB with multiple column families
public class ColumnFamilyDemo2 {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException, IOException {
        String dbPath = "/tmp/rocksdb_columnfamily_demo"; // create a folder for RocksDB

        Set<String> columnFamilies = listCFs(dbPath);
        System.out.println(columnFamilies); // let print out column families

        // open DB with column families
        final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();
        for(String columnFamily : columnFamilies) {
            columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamily.getBytes(UTF_8), new ColumnFamilyOptions()));
        }

        // prepare a empty ColumnFamilyHandles list collection
        final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

        // let's try to create all the column families
        try(final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true);
            final RocksDB db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptors, columnFamilyHandles)) {

        }

        // ** make sure you disposal necessary RocksDB objects ** //
        for(final ColumnFamilyHandle hanlde : columnFamilyHandles)
            hanlde.close();

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }

    // retrieve all column families' name set
    public static Set<String> listCFs(String path) throws RocksDBException {
        Set<String> cfs = new HashSet<>();
        List<byte[]> oldCFs = RocksDB.listColumnFamilies(new Options(), path);
        if (oldCFs.isEmpty()) {
            cfs.add("default");
        } else {
            for (byte[] oldCF : oldCFs) {
                cfs.add(new String(oldCF, UTF_8));
            }
        }
        return cfs;
    }
}

package io.github.erhwenkuo.rocksdb.lab1;

import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.Properties;

public class RocksDBOptions {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        // Setup RocksDB Options via Properties
        final Properties properties = new Properties();
        properties.put("allow_mmap_reads", "true");
        properties.put("bytes_per_sync", "13");
        final DBOptions dbOptions = DBOptions.getDBOptionsFromProps(properties);

        // make sure you disposal necessary RocksDB objects
        dbOptions.close();
    }
}

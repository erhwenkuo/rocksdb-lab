package io.github.erhwenkuo.rocksdb.lab1;

import org.rocksdb.*;

public class OpenDatabase {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Options options = new Options();
        options.setCreateIfMissing(true);
        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}

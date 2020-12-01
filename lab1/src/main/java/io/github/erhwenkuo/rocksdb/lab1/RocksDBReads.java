package io.github.erhwenkuo.rocksdb.lab1;

import org.rocksdb.*;

public class RocksDBReads {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Options options = new Options();
        options.setCreateIfMissing(true);
        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        String key1 = "test";
        String key2 = "test2";
        String value = "hello world!";

        db.put(key1.getBytes(), value.getBytes());
        byte[] value2 = db.get(key1.getBytes());
        db.put(key2.getBytes(), value2);
        db.delete(key1.getBytes());

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}

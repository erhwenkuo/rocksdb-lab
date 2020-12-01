package io.github.erhwenkuo.rocksdb.lab1;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class IterationReverse {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Options options = new Options();
        options.setCreateIfMissing(true);
        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        for (int i = 1; i <= 99; ++i) {
            for (int j = 1; j <= 99; ++j) {
                db.put(String.format("%dx%d", i, j).getBytes(),
                        String.format("%d", i * j).getBytes());
            }
        }

        // construct an iterator to iterate key/value
        try (final RocksIterator iterator = db.newIterator()) {
            byte[] lastKey;
            byte[] lastValue;

            for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
                lastKey = iterator.key();
                lastValue = iterator.value();
                System.out.println(new String(lastKey) + ": " + new String(lastValue));
            }
        }
        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}

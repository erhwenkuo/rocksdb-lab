package io.github.erhwenkuo.rocksdb.lab1;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class IterationSeekForPrev {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Options options = new Options();
        options.setCreateIfMissing(true);
        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        for (int i = 1; i <= 9; ++i) {
            for (int j = 1; j <= 9; ++j) {
                db.put(String.format("%dx%d", i, j).getBytes(),
                        String.format("%d", i * j).getBytes());
            }
        }

        // This is an example of processing entries in range (limit, start]
        // in reverse order from one specific key:
        String start = "8";
        String limit = "7";

        try (final RocksIterator iterator = db.newIterator()) {
            for(iterator.seekForPrev(start.getBytes()); iterator.isValid() &&
                    ByteArraysCompare(iterator.key(), limit.getBytes())>0
                    ; iterator.prev()) {
                byte[] lastKey = iterator.key();
                byte[] lastValue = iterator.value();
                System.out.println(new String(lastKey) + ": " + new String(lastValue));

            }
        }
        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }

    // Natural order byte[] comparator
    public static int ByteArraysCompare(byte[] b1, byte[] b2) {
        int b1Length = b1.length;
        int b2Length = b2.length;
        int maxLenth = Math.min(b1Length, b2Length);
        for(int i=0; i < maxLenth; i++) {
            byte b1Byte = b1[i];
            byte b2Byte = b2[i];
            if (b1Byte!=b2Byte) {
                return Byte.compare(b1Byte, b2Byte);
            }
        }
        return b1Length - b2Length;
    }
}

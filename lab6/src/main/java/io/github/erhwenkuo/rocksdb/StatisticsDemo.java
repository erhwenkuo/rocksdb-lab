package io.github.erhwenkuo.rocksdb;

import org.rocksdb.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StatisticsDemo {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Statistics statistics = new Statistics();
        final Options options = new Options();

        options.setCreateIfMissing(true);
        options.setStatistics(statistics);

        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        final byte[] key = "some-key".getBytes(UTF_8);
        final byte[] value = "some-value".getBytes(UTF_8);

        db.put(key, value);
        for(int i = 0; i < 10; i++) {
            db.get(key);
        }

        System.out.println(statistics);

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}

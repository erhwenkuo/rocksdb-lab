package io.github.erhwenkuo.rocksdb.lab2;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Snapshots {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException, IOException {
        Files.createDirectories(Paths.get("/tmp/testdb")); // create a folder for RocksDB
        final Options options = new Options();
        options.setCreateIfMissing(true);
        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        // put some k/v to RocksDB 5x5
        for (int i = 1; i <= 5; ++i) {
            for (int j = 1; j <= 5; ++j) {
                db.put(String.format("%dx%d", i, j).getBytes(),
                        String.format("%d", i * j).getBytes());
            }
        }

        // get new Snapshot of database
        try (final Snapshot snapshot = db.getSnapshot()) {

            try (final ReadOptions readOptions = new ReadOptions()) {
                // set snapshot in ReadOptions
                readOptions.setSnapshot(snapshot);

                // retrieve key value pair
                byte[] value = db.get("1x1".getBytes());
                System.out.println("[Current] 1x1 = " + new String(value)); // expect '1'

                // retrieve key valBasic operationsue pair created before
                // the snapshot was made
                value = db.get(readOptions, "1x1".getBytes());
                System.out.println("[Before snapshot] 1x1 = " + new String(value)); // expect '1' as well

                // add new key/value pair
                db.put("1x1".getBytes(), "1-change-after-snapshot".getBytes());

                // using no snapshot the latest db entries will be taken into account
                value = db.get("1x1".getBytes());
                System.out.println("[Current] 1x1 = " + new String(value)); // expect '1-change-after-snapshot'

                // snapshot was created before key: '1x1' is set as value: '1'
                value = db.get(readOptions, "1x1".getBytes());
                System.out.println("[Before snapshot] 1x1 = " + (value == null? "null": new String(value)));

                System.out.println("");

                // iterate over current state of db
                System.out.println("***** iterate over current state of db *****");
                try (final RocksIterator iterator = db.newIterator()) {
                    byte[] lastKey;
                    byte[] lastValue;

                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        lastKey = iterator.key();
                        lastValue = iterator.value();
                        System.out.println(new String(lastKey) + ": " + new String(lastValue));
                    }
                }

                // iterate using a snapshot
                System.out.println("***** iterate using a snapshot *****");
                try (final RocksIterator iterator = db.newIterator(readOptions)) {
                    byte[] lastKey;
                    byte[] lastValue;

                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        lastKey = iterator.key();
                        lastValue = iterator.value();
                        System.out.println(new String(lastKey) + ": " + new String(lastValue));
                    }
                }

                // put more k/v to RocksDB
                for (int i = 6; i <= 9; ++i) {
                    for (int j = 6; j <= 9; ++j) {
                        db.put(String.format("%dx%d", i, j).getBytes(),
                                String.format("%d", i * j).getBytes());
                    }
                }

                // iterate over current state of db
                System.out.println("***** iterate over current state of db with full 9x9 *****");
                try (final RocksIterator iterator = db.newIterator()) {
                    byte[] lastKey;
                    byte[] lastValue;

                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        lastKey = iterator.key();
                        lastValue = iterator.value();
                        System.out.println(new String(lastKey) + ": " + new String(lastValue));
                    }
                }

                // iterate using a snapshot
                System.out.println("***** iterate using a snapshot *****");
                try (final RocksIterator iterator = db.newIterator(readOptions)) {
                    byte[] lastKey;
                    byte[] lastValue;

                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        lastKey = iterator.key();
                        lastValue = iterator.value();
                        System.out.println(new String(lastKey) + ": " + new String(lastValue));
                    }
                }

                // setting null to snapshot in ReadOptions leads
                // to no Snapshot being used.
                readOptions.setSnapshot(null);
                // release Snapshot
                db.releaseSnapshot(snapshot);
            }
        }

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File("/tmp/testdb"));
    }
}

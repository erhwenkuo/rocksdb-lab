# Lab 2 - RocksDB Snapshot

[Chinses version](lab2_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab2/) **

## Snapshots

Snapshots provide consistent read-only views over the entire state of the key-value store. `ReadOptions::snapshot` may be non-NULL to indicate that a read should operate on a particular version of the DB state.

If `ReadOptions::snapshot` is NULL, the read will operate on an implicit snapshot of the current state.

Snapshots are created by the `DB::GetSnapshot()` method:

```java

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
```

> Note that when a snapshot is no longer needed, it should be released using the DB::ReleaseSnapshot interface. This allows the implementation to get rid of state that was being maintained just to support reading as of that snapshot.

Run the sample code `Snapshots.java`, you should be able to see below results:

```sh
[Current] 1x1 = 1
[Before snapshot] 1x1 = 1
[Current] 1x1 = 1-change-after-snapshot
[Before snapshot] 1x1 = 1

***** iterate over current state of db *****
1x1: 1-change-after-snapshot
1x2: 2
1x3: 3
1x4: 4
1x5: 5
2x1: 2
2x2: 4
2x3: 6
2x4: 8
2x5: 10
3x1: 3
3x2: 6
3x3: 9
3x4: 12
3x5: 15
4x1: 4
4x2: 8
4x3: 12
4x4: 16
4x5: 20
5x1: 5
5x2: 10
5x3: 15
5x4: 20
5x5: 25

***** iterate using a snapshot *****
1x1: 1
1x2: 2
1x3: 3
1x4: 4
1x5: 5
2x1: 2
2x2: 4
2x3: 6
2x4: 8
2x5: 10
3x1: 3
3x2: 6
3x3: 9
3x4: 12
3x5: 15
4x1: 4
4x2: 8
4x3: 12
4x4: 16
4x5: 20
5x1: 5
5x2: 10
5x3: 15
5x4: 20
5x5: 25

***** iterate over current state of db with full 9x9 *****
1x1: 1-change-after-snapshot
1x2: 2
1x3: 3
1x4: 4
1x5: 5
2x1: 2
2x2: 4
2x3: 6
2x4: 8
2x5: 10
3x1: 3
3x2: 6
3x3: 9
3x4: 12
3x5: 15
4x1: 4
4x2: 8
4x3: 12
4x4: 16
4x5: 20
5x1: 5
5x2: 10
5x3: 15
5x4: 20
5x5: 25
6x6: 36
6x7: 42
6x8: 48
6x9: 54
7x6: 42
7x7: 49
7x8: 56
7x9: 63
8x6: 48
8x7: 56
8x8: 64
8x9: 72
9x6: 54
9x7: 63
9x8: 72
9x9: 81

***** iterate using a snapshot *****
1x1: 1
1x2: 2
1x3: 3
1x4: 4
1x5: 5
2x1: 2
2x2: 4
2x3: 6
2x4: 8
2x5: 10
3x1: 3
3x2: 6
3x3: 9
3x4: 12
3x5: 15
4x1: 4
4x2: 8
4x3: 12
4x4: 16
4x5: 20
5x1: 5
5x2: 10
5x3: 15
5x4: 20
5x5: 25
```

The snapshot feature service like a time machine which enable us to find the status of data of specific point in time.

Back to main menu >>  [README](README.md)
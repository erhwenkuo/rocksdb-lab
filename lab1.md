# Lab 1 - Basic operations

[Chinses version](lab1_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

The `rocksdb` library provides a persistent key value store. Keys and values are `arbitrary byte arrays`. The keys are ordered within the key value store according to a user-specified comparator function.

## Opening A Database

A `rocksdb` database has a name which corresponds to a file system directory. All of the contents of database are stored in this directory. The following example shows how to open a database, creating it if necessary:

```java
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
```

If you want to raise an error if the database already exists, add the following line before the `RocksDB.open` call:

```java
options.setErrorIfExists(true);
```

## RocksDB Options

Users can choose to always set options fields explicitly in code, as shown above. lternatively, you can also set it through a string to string map, or an option string. See [Option String and Option Map](https://github.com/facebook/rocksdb/wiki/Option-String-and-Option-Map).

```java
// setup sample properties
final Properties properties = new Properties();
properties.put("allow_mmap_reads", "true");
properties.put("bytes_per_sync", "13");

final DBOptions opt = DBOptions.getDBOptionsFromProps(properties)
```

RocksDB automatically keeps options used in the database in OPTIONS-xxxx files under the DB directory. Users can choose to preserve the option values after DB restart by extracting options from these option files. See [RocksDB Options File](https://github.com/facebook/rocksdb/wiki/RocksDB-Options-File).


## Closing A Database

When you are done with a database, make sure you gracefully close the database - Call `db.close()` and release any of the RocksDB objects.

Example:

```java
... open the db as described above ...
... do something with db ...

// make sure you disposal necessary RocksDB objects
db.close();
options.close();
```

## Reads

The database provides `Put`, `Delete`, `Get`, and `MultiGet` methods to modify/query the database. For example, the following code moves the value stored under key1 to key2.

```java
String key1 = "test";
String key2 = "test2";
String value = "hello world!";

db.put(key1.getBytes(), value.getBytes());
byte[] value2 = db.get(key1.getBytes());
db.put(key2.getBytes(), value2);
db.delete(key1.getBytes());
```

Right now, value size must be smaller than 4GB. 

## Writes

### Atomic Updates

Note that if the process dies after the Put of key2 but before the delete of key1, the same value may be left stored under multiple keys. Such problems can be avoided by using the WriteBatch class to atomically apply a set of updates:

```java
import org.rocksdb.*;

public class RocksDBAtomicUpdate {
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

        // make sure below two operations are atomic updated
        try (final WriteBatch writeBatch = new WriteBatch()) {
          writeBatch.put(key2.getBytes(), value2);
          writeBatch.delete(key1.getBytes());          
          db.write(new WriteOptions(), writeBatch);
        }

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}

```

The `WriteBatch` holds a sequence of edits to be made to the database, and these edits within the batch are applied in order. Note that we called `Delete` before Put so that if key1 is identical to key2, we do not end up erroneously dropping the value entirely.

Apart from its atomicity benefits, `WriteBatch` may also be used to speed up bulk updates by placing lots of individual mutations into the same batch.

## Concurrency

A database may only be opened by one process at a time. The `rocksdb` implementation acquires a lock from the operating system to prevent misuse. Within a single process, the same `RocksDB db` object may be safely shared by multiple concurrent threads. I.e., different threads may write into or fetch iterators or call `Get` on the same database without any external synchronization (the rocksdb implementation will automatically do the required synchronization). 

However other objects (like `Iterator` and `WriteBatch`) may require external synchronization. If two threads share such an object, they must protect access to it using their own locking protocol. More details are available in the public header files.

## Iteration

The following example demonstrates how to print all (key, value) pairs in a database.

```java
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class RocksDBIteration {
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
            
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
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
```

The following variation shows how to process just the keys in the range [start, limit):

```java

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class RocksDBIterationRange {
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

        String start = "8";
        String limit = "9";

        try (final RocksIterator iterator = db.newIterator()) {
            iterator.seek(start.getBytes());
            for(iterator.seek(start.getBytes()); iterator.isValid() &&
                    ByteArraysCompare(iterator.key(), limit.getBytes())<0
                    ; iterator.next()) {
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
```

You can also process entries in reverse order. (Caveat: reverse iteration may be somewhat slower than forward iteration.)

```java

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

public class RocksDBIterationReverse {
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
```

This is an example of processing entries in range (limit, start] in reverse order from one specific key:

```java

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
```

See [SeekForPrev](https://github.com/facebook/rocksdb/wiki/SeekForPrev).


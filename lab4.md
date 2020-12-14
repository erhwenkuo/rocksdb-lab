# Lab 4 - RocksDB Column Families

[Chinses version](lab4_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab4/) **

## Introduction

In RocksDB 3.0, it supports **Column Families**. Each key-value pair in RocksDB is associated with exactly one Column Family. If there is no Column Family specified, key-value pair is associated with Column Family "`default`".

Column Families provide a way to logically partition the database. Some interesting properties:

* Atomic writes across Column Families are supported. This means you can atomically execute Write({cf1, key1, value1}, {cf2, key2, value2}).
* Consistent view of the database across Column Families.
* Ability to configure different Column Families independently.
* On-the-fly adding new Column Families and dropping them. Both operations are reasonably fast.

```java
import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ColumnFamilyDemo {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException, IOException {
        String dbPath = "/tmp/rocksdb_columnfamily_demo"; // create a folder for RocksDB

        try(final Options options = new Options().setCreateIfMissing(true);
            final RocksDB db = RocksDB.open(options, dbPath)) {
            // create column family
            try(final ColumnFamilyHandle columnFamilyHandle = db.createColumnFamily(
                    new ColumnFamilyDescriptor("new_cf".getBytes(),
                            new ColumnFamilyOptions()))) {
                assert (columnFamilyHandle != null);
            }
        }

        // open DB with two column families
        final List<ColumnFamilyDescriptor> columnFamilyDescriptors =
                new ArrayList<>();

        // have to open default column family
        columnFamilyDescriptors.add(new ColumnFamilyDescriptor(
                RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));

        // open the new one, too
        columnFamilyDescriptors.add(new ColumnFamilyDescriptor(
                "new_cf".getBytes(), new ColumnFamilyOptions()));

        // prepare a empty ColumnFamilyHandles list collection
        final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

        final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true);
        // RocksDB will fill in ColumnFamilyHandles collection once successfully created/opened
        final RocksDB db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptors, columnFamilyHandles);

        final Map<String, ColumnFamilyHandle> columnFamilyHandleMap = new HashMap<>();

        // keep column family in a map
        for(ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles)
            columnFamilyHandleMap.put(new String(columnFamilyHandle.getName()), columnFamilyHandle);

        ColumnFamilyHandle defaultCF = columnFamilyHandleMap.get("default");
        ColumnFamilyHandle newCF = columnFamilyHandleMap.get("new_cf");


        // put and get from non-default column family
        db.put(newCF,"key".getBytes(UTF_8), "value".getBytes(UTF_8));

        // atomic write
        try (final WriteBatch wb = new WriteBatch()) {
            wb.put(defaultCF, "key2".getBytes(UTF_8), "value2".getBytes(UTF_8));
            wb.put(newCF, "key3".getBytes(UTF_8), "value3".getBytes(UTF_8));
            wb.delete(defaultCF, "key".getBytes(UTF_8));
            db.write(new WriteOptions(), wb);
        }

        // drop column family
        db.dropColumnFamily(newCF);

        // ** make sure you disposal necessary RocksDB objects ** //
        for(final ColumnFamilyHandle hanlde : columnFamilyHandles)
            hanlde.close();

        db.close();
        dbOptions.close();

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }
}
```

## Reference

### Options, ColumnFamilyOptions, DBOptions

`Options` structures define how RocksDB behaves and performs. Options specific to a single Column Family will be defined in `ColumnFamilyOptions` and options specific to the whole RocksDB instance will be defined in `DBOptions`.

### ColumnFamilyHandle

Column Families are handled and referenced with a `ColumnFamilyHandle`. Think of it as a open file descriptor. You need to delete all ColumnFamilyHandles before you delete your DB pointer. One interesting thing: Even if `ColumnFamilyHandle` is pointing to a dropped Column Family, you can continue using it. The data is actually deleted only after you delete all outstanding ColumnFamilyHandles.

```java
// open DB with two column families
final List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();

// have to open default column family
columnFamilyDescriptors.add(new ColumnFamilyDescriptor(
        RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));

// open the new one, too
columnFamilyDescriptors.add(new ColumnFamilyDescriptor(
        "new_cf".getBytes(), new ColumnFamilyOptions()));

// prepare a empty ColumnFamilyHandles list collection
final List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();

final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true);

// RocksDB will fill in ColumnFamilyHandles collection once successfully created/opened
final RocksDB db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptors, columnFamilyHandles);
```

When opening a DB in a read-write mode, you need to specify all Column Families that currently exist in a DB. If that's not the case, `DB::Open` call will return `Status::InvalidArgument()`. You specify Column Families with a vector of `ColumnFamilyDescriptors`. ColumnFamilyDescriptor is just a struct with a Column Family name and ColumnFamilyOptions. Open call will return a Status and also a vector of pointers to ColumnFamilyHandles, which you can then use to reference Column Families. Make sure to delete all `ColumnFamilyHandles` before you delete the DB pointer.

```java

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
```

`ListColumnFamilies` is a static function that returns the list of all column families currently present in the DB.

```java
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
```

Creates a Column Family specified with option and a name and returns ColumnFamilyHandle through an argument.

```java
final RocksDB db = RocksDB.open(options, dbPath)) {
// create column family
try(final ColumnFamilyHandle columnFamilyHandle = db.createColumnFamily(
        new ColumnFamilyDescriptor("new_cf".getBytes(),
                new ColumnFamilyOptions()))) {
    assert (columnFamilyHandle != null);
}
```

Drop the column family specified by ColumnFamilyHandle. Note that the actual data is not deleted until the client calls delete column_family;. You can still continue using the column family if you have outstanding ColumnFamilyHandle pointer.

```java
// drop column family
db.dropColumnFamily(newCF);
```

### WriteBatch

To execute multiple writes atomically, you need to build a WriteBatch. All WriteBatch API calls now also take ColumnFamilyHandle* to specify the Column Family you want to write to.

```java
// atomic write
try (final WriteBatch wb = new WriteBatch()) {
    wb.put(defaultCF, "key2".getBytes(UTF_8), "value2".getBytes(UTF_8));
    wb.put(newCF, "key3".getBytes(UTF_8), "value3".getBytes(UTF_8));
    wb.delete(defaultCF, "key".getBytes(UTF_8));
    db.write(new WriteOptions(), wb);
}
```

## Implementation

The main idea behind Column Families is that they `share` the **write-ahead log** and `don't share` **memtables** and **table files**. By sharing write-ahead logs we get awesome benefit of atomic writes. By separating memtables and table files, we are able to configure column families independently and delete them quickly.

Every time a single Column Family is flushed, we create a new WAL (write-ahead log). All new writes to all Column Families go to the new WAL. However, we still can't delete the old WAL since it contains live data from other Column Families. We can delete the old WAL only when all Column Families have been flushed and all data contained in that WAL persisted in table files. 

This created some interesting implementation details and will create interesting tuning requirements. Make sure to tune your RocksDB such that all column families are regularly flushed. Also, take a look at `Options::max_total_wal_size`, which can be configured such that stale column families are automatically flushed.


Back to main menu >>  [README](README.md)

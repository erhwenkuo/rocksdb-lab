# Lab 4 - RocksDB Column Families

[English version](lab4.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab4/) **

## Introduction

自在RocksDB3.0版本之後，它增加了對**Column Families**的支援。RocksDB的每個鍵值對都會關聯到一個列族(Column Family)。如果沒有指定Column Family，鍵值對將會關聯到`default`這個預設的列族。

列族提供了一種從邏輯上切割數據庫的方法。它的一些有趣的特性包括:

* 支援跨列族原子性的寫入。意味著你可以原子性地執行 `Write({cf1, key1, value1}, {cf2, key2, value2})`
* 跨列族的一致性資料視圖
* 允許對不同的列族進行不同的配置
* 即時添加／刪除列族。這兩類的操作都是非常快的

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

`Options`結構定義RocksDB的行為和性能。對單個列族的配置，會被定義在`ColumnFamilyOptions`，然後那些針對整個RocksDB實例的配置會被定義在`DBOptions`。

### ColumnFamilyHandle

列族通過`ColumnFamilyHandle`來進行調用和引用。你可以把它當成一個打開的文件描述符。在你刪除DB指針前，你需要刪除所有所有的ColumnFamilyHandle實例。數據只有在你將所有存在的ColumnFamilyHandle都刪除了之後才會被清除。

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

當使用讀寫模式打開一個RocksDB的時候，你需要聲明所有已經存在於DB的列族。不然`DB::Open`會返回`Status::InvalidArgument()`，你可以用一個`ColumnFamilyDescriptors`的陣列來聲明列族。`ColumnFamilyDescriptors`是一個只有列族名和`ColumnFamilyOptions`的結構體。Open會返回一個Status以及一個ColumnFamilyHandle指針的陣列，你可以用它們來引用這些列族。刪除DB指針前請確保你已經刪除了所有`ColumnFamilyHandle`。

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

`ListColumnFamilies`是一個靜態方法，它會返回當前DB裡面存在的列族。

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

指定一個名字和配置來創建一個列族，然後在參數里面會返回一個`ColumnFamilyHandle`。

```java
final RocksDB db = RocksDB.open(options, dbPath)) {
// create column family
try(final ColumnFamilyHandle columnFamilyHandle = db.createColumnFamily(
        new ColumnFamilyDescriptor("new_cf".getBytes(),
                new ColumnFamilyOptions()))) {
    assert (columnFamilyHandle != null);
}
```

要刪除列族必需經由`ColumnFamilyHandle`。注意，實際的數據在客戶端調用`delete column_family`之前 並不會被刪除。只要你還有ColumnFamilyHandle參照，你就還可以繼續使用這個列族。

```java
// drop column family
db.dropColumnFamily(newCF);
```

### WriteBatch

你需要構建一個`WriteBatch`來實現原子性的批量寫操作。所有WriteBatch API現在可以指定一個`ColumnFamilyHandle`指針來聲明你希望寫到哪個列族。

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

列族的主要實現思想是它們共享一個**write-ahead log**，但是不共享**memtable**和**table文件**。通過共享**write-ahead log**，它實現了跨列族的原子性寫入操作。通過隔離memtable和table文件，它可以獨立配置每個列族並且快速刪除它們。

每當一個單獨的列族將資料回寫到硬碟，RocksDB會創建一個新的WAL(**write-ahead log**)。所有列族的所有新的寫入都會導到新的WAL文件。但是，我們還不能刪除舊的WAL，因為他還有一些對其他列族有用的數據。我們只能在所有的列族都把這個WAL裡的資料回寫到硬碟了，才能刪除這個WAL文件。這帶來了一些有趣的實現細節以及一些調優需求。你得確保所有列族都會有規律地將資料回寫到硬碟。另外，看一下`Options::max_total_wal_size`，通過配置這個參數，過期的列族能自動將數據回寫到硬碟。

返回主目錄 >>  [README](README_zh-tw.md)

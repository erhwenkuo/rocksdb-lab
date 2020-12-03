# Lab 1 - Basic operations

[English version](lab1.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [範例程式碼](lab1/) **

Rocksdb函式庫提供一個持久化的鍵值存儲數據庫。鍵和值都可以是任意二進制數組。所有的鍵在RocksDB中是根據一個用戶定義的比較函數（user-specified comparator function）來進行排列。

## Opening A Database

一個`rocksdb`數據庫會有一個與系統檔案目錄來與之關聯。所有與該數據庫有關的內容都會存儲在那個目錄裡。下面的範例將展現如何打開一個數據庫，若數據庫不存在的時候創建它:

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

如果你希望在數據庫存在的時候返回錯誤，在調用`RocksDB.open`前，增加下面這行設定:

```java
options.setErrorIfExists(true);
```

## RocksDB Options

如上所示，用戶可以選擇在程式碼裡明確設置相關的選項。或者，在Java你可以通過`Properties`物件來進行設置。

```java
// setup sample properties
final Properties properties = new Properties();
properties.put("allow_mmap_reads", "true");
properties.put("bytes_per_sync", "13");

final DBOptions opt = DBOptions.getDBOptionsFromProps(properties)
```

`RocksDB`自動將當前數據庫使用的配置保存到數據庫目錄下的`OPTIONS-xxx`文件。你可以選擇在數據庫重啟之後從配置文件導出選項，以此來保存配置。參考[RocksDB配置文件](https://github.com/facebook/rocksdb/wiki/RocksDB-Options-File)。

## Closing A Database

完成數據庫操作後，請確保正常關閉數據庫`db.close()` 並釋放任何RocksDB對象。

範例:

```java
... open the db as described above ...
... do something with db ...

// make sure you disposal necessary RocksDB objects
db.close();
options.close();
```

## Reads

數據庫提供`Put`,`Delete`以及`Get`方法用於修改/查詢數據庫。例如，下面的程式碼將key1的值，轉移到key2。

```java
String key1 = "test";
String key2 = "test2";
String value = "hello world!";

db.put(key1.getBytes(), value.getBytes());
byte[] value2 = db.get(key1.getBytes());
db.put(key2.getBytes(), value2);
db.delete(key1.getBytes());
```

目前，`值`的大小必須小於4GB。 RocksDB還允許使用[Single Delete](https://github.com/facebook/rocksdb/wiki/Single-Delete)，這個功能在特定場景很有用。

## Writes

### Atomic Updates

如果進程在key2的Put調用之後，在刪除key1之前，崩潰了，那麼相同的`值`會被保存到多個`鍵`下面。這種問題可以通過`WriteBatch`來原子地寫入進行更新：

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

`WriteBatch`保存一個數據庫編輯指令序列，這些批處理的修改會被按順序應用於數據庫上。除了原子化交易的功能之外，`WriteBatch`還可以通過將大量的單獨修改合併到一個批處理，以此來加速在數據庫上批量的更新效率。

## Concurrency

一個數據庫可能同時只能被一個進程打開。RocksDB的實現方式是，從操作系統那裡申請一個鎖，以此來阻止錯誤的寫操作。在單進程裡面，同一個`RocksDB db`對象可以被多個線程共享。舉個例子，不同的線程可以同時對同一個數據庫調用寫操作，迭代遍歷操作或者Get操作，而且不用使用額外的同步鎖(rocksdb的實現會自動進行同步)。然而其他對象(比如`Iterator`或`WriteBatch`)則需要額外的同步機制保證線程同步。如果兩個線程共同使用這些對象，他們必須使用自己的鎖協議保證訪問的同步。

## Iteration

下面的範例展示如何打印一個數據庫裡的所有鍵值對(key, value)。

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

以下範例顯示如何處理[start, limit)範圍內的鍵值對：

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

你還可以通過反向的順序處理這些鍵值對 (注意:反向迭代器會比正向迭代器慢一些)。

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

這裡有一個範例展示如何以逆序處理一個範圍(limit,start]的鍵值對：

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

參考 [SeekForPrev](https://github.com/facebook/rocksdb/wiki/SeekForPrev)。

返回主目錄 >>  [README](README_zh-tw.md)
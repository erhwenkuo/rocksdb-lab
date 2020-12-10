# Lab 3 - RocksDB Transaction

[Chinses version](lab3_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab3/) **

當使用TransactionDB或者OptimisticTransactionDB的時候，RocksDB將支持事務。事務帶有簡單的**BEGIN**/**COMMIT**/**ROLLBACK** API，並且允許應用併發地修改數據，具體的衝突檢查，由Rocksdb來處理。 RocksDB支持悲觀和樂觀的併發控制。

注意，當通過`WriteBatch`寫入多個key的時候，RocksDB提供原子化操作。事務提供了一個方法，來保證他們只會在沒有衝突的時候被提交。跟`WriteBatch`很類似的是只有當一個事務被提交，其他線程才能看到被修改的內容（讀committed）。

當一個TransactionDB在有大量併發工作壓力的時候，相比OptimisticTransactionDB, 它有更好的表現。然而，由於比較嚴格的上鎖策略，使用TransactionDB會有一定的性能損耗。 TransactionDB會在所有`寫`操作的時候做衝突檢查，包括不使用事務寫入的時候。

上鎖超時和限制可以通過`TransactionDBOptions`進行調優。

以下範例演示如何將`TransactionDB`與**BEGIN**/**COMMIT**/**ROLLBACK**操作結合使用：

```java
import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TransactionDemo {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException, IOException {
        String dbPath = "/tmp/rocksdb_transaction_demo"; // create a folder for RocksDB

        final Options options = new Options();
        options.setCreateIfMissing(true);

        final TransactionDBOptions txnDbOptions = new TransactionDBOptions();
        final TransactionDB txnDb = TransactionDB.open(options, txnDbOptions, dbPath);

        final WriteOptions writeOptions = new WriteOptions();
        final ReadOptions readOptions = new ReadOptions();

        // ** transansaction operations demo ** //

        // Start a transaction
        final Transaction txn = txnDb.beginTransaction(writeOptions);
        try {
            txn.put("key".getBytes(UTF_8), "value".getBytes(UTF_8));
            txn.delete("key2".getBytes(UTF_8));
            txn.merge("key3".getBytes(UTF_8), "value".getBytes(UTF_8));

            // Commit transaction
            txn.commit();
        } catch (RocksDBException e){
            System.out.printf("Status: %s, Error: %s", e.getStatus().getCode(), e.getMessage());

            // Rollback transaction
            txn.rollback();
        } finally {
            // Release txn object resource
            txn.close();
        }

        // ** make sure you disposal necessary RocksDB objects ** //
        readOptions.close();
        writeOptions.close();
        txnDb.close();
        txnDbOptions.close();
        options.close();
    }
```

默認的寫策略是`Write Committed`。還可以選擇`Write Prepared`和`WriteUnprepared`。更多內容請參考[這裡](https://github.com/facebook/rocksdb/wiki/WritePrepared-Transactions)。


## OptimisticTransactionDB

樂觀事務提供輕量級的樂觀併發控制給那些多個事務間不會有過高競爭/干涉的工作場景。

樂觀事務在預備寫的時候不使用任何鎖。作為替代，他們把這個操作推遲到在提交的時候檢查，是否有其他人修改了正在進行的事務。如果和另一個寫入有衝突（或者他無法做決定），提交會返回錯誤，並且沒有任何key都不會被寫入。

樂觀的併發控制在處理那些偶爾出現的寫衝突非常有效。然而，對於那些大量事務對同一個key寫入導致寫衝突頻繁發生的場景，卻不是一個好主意。對於這些場景，使用TransactionDB是更好的選擇。OptimisticTransactionDB在大量非事務寫入，而少量事務寫入的場景，會比TransactionDB性能更好。

```java

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OptimisticTransactionDemo {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException, IOException {
        String dbPath = "/tmp/rocksdb_optimistic_transaction_demo"; // create a folder for RocksDB

        final Options options = new Options();
        options.setCreateIfMissing(true);
        final OptimisticTransactionDB txnDb = OptimisticTransactionDB.open(options, dbPath);

        final WriteOptions writeOptions = new WriteOptions();
        final ReadOptions readOptions = new ReadOptions();

        // ** transansaction operations demo ** //

        // Start a transaction
        final Transaction txn = txnDb.beginTransaction(writeOptions);
        try {
            txn.put("key".getBytes(UTF_8), "value".getBytes(UTF_8));
            txn.delete("key2".getBytes(UTF_8));
            txn.merge("key3".getBytes(UTF_8), "value".getBytes(UTF_8));

            // Commit transaction
            txn.commit();
        } catch (RocksDBException e){
            System.out.printf("Status: %s, Error: %s", e.getStatus().getCode(), e.getMessage());

            // Rollback transaction
            txn.rollback();
        } finally {
            // Release txn object resource
            txn.close();
        }

        // ** make sure you disposal necessary RocksDB objects ** //
        readOptions.close();
        writeOptions.close();
        txnDb.close();
        options.close();

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }
}
```

## Reading from a Transaction

事務對當前事務中已經批量修改，但是還沒有提交的key提供簡單的讀取操作。

```java
// Start a transaction
final Transaction txn = txnDb.beginTransaction(writeOptions);
// ** Reading from a Transaction ** //
txnDb.put(writeOptions, "a".getBytes(UTF_8), "old".getBytes(UTF_8));
txnDb.put(writeOptions, "b".getBytes(UTF_8), "old".getBytes(UTF_8));
txn.put("a".getBytes(UTF_8), "new".getBytes(UTF_8));

final List<byte[]> keys = new ArrayList<>();
keys.add("a".getBytes(UTF_8));
keys.add("b".getBytes(UTF_8));

final byte[][] keysArray = keys.toArray(new byte[keys.size()][]);
List<byte[]> values = Arrays.asList(txn.multiGet(readOptions, keysArray));

//  The value returned for key “a” will be “new” since it was written by this transaction.
//  The value returned for key “b” will be “old” since it is unchanged in this transaction.
for(int i=0; i<keys.size(); i++) {
    System.out.printf("key:%s, value:%s\n", new String(keys.get(i)), new String(values.get(i)));
}

// Commit transaction
txn.commit();
```

執行範例程式你應該可看到如下結果：

```sh
key:a, value:new
key:b, value:old
```

使用Transaction::GetIterator()，你還可遍歷那些已經存在db的鍵以及當前事務的鍵值。

## Guarding against Read-Write Conflicts:

`GetForUpdate()`會保證沒有其他寫入者會修改任何被這個事務讀出的key。

```java
// Start a transaction
final Transaction txn = txnDb.beginTransaction(writeOptions);

// Read key1 in this transaction
byte[] value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8),true);

// Write to key1 OUTSIDE of the transaction
txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));
```

如果此事務是由`TransactionDB`創建的，則`Put`將會　_timeout_　或被　_block_，直到事務被提交或中止。

如果此事務是由`OptimisticTransactionDB()`創建的，則`Put`將成功執行，但是如果調用ttxn->Commit()，則該事務將不會成功。

```java
// Start a transaction
final Transaction txn = txnDb.beginTransaction(writeOptions);

// Read key1 in this transaction
byte[] value = txn.get(readOptions, "key1".getBytes(UTF_8));

// Write to key1 OUTSIDE of the transaction
txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));

// No conflict since transactions only do conflict checking for keys read using GetForUpdate().
txn.commit();
```
當前，GetForUpdate()是避開`讀寫衝突`的唯一方法，因此可以與迭代器結合使用。

## Setting a Snapshot

預設上，事務衝突檢測會校準沒有其他人在事務第一次修改這個鍵之後，才對這個鍵進行修改。這樣的解決方案在大多數場景都是足夠的。然而，你可能還希望保證沒有其他人在事務開始之後，對這個鍵值進行修改。那麼可以通過創建事務後調用`SetSnapshot()`來實現。

預設行為：

```java
// Create a txn using either a TransactionDB or OptimisticTransactionDB
final Transaction txn = txnDb.beginTransaction(writeOptions);

// Write to key1 OUTSIDE of the transaction
txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));

// Write to key1 IN transaction
txn.put("key1".getBytes(UTF_8), "value1".getBytes(UTF_8));
txn.commit();

// There is no conflict since the write to key1 outside of the transaction happened before
// it was written in this transaction.
```

使用SetSnapshot()：

```java
// Create a txn using either a TransactionDB or OptimisticTransactionDB
final Transaction txn = txnDb.beginTransaction(writeOptions);
txn.setSnapshot();

// Write to key1 OUTSIDE of the transaction
txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));

// Write to key1 IN transaction
txn.put("key1".getBytes(UTF_8), "value1".getBytes(UTF_8));
txn.commit();

// Transaction will NOT commit since key1 was written outside of this transaction
// after SetSnapshot() was called (even though this write occurred before this key
// was written in this transaction).
```
> 注意，在上面的範例，如果這是一個TransactionDB，`Put()`會失敗。如果是OptimisticTransactionDB，`Commit()`會失敗。

## Repeatable Read

於一般的RocksDB讀取的作業相似，可以在`ReadOptions`指定一個`Snapshot`來保證事務中的讀是可重複讀。

```java
// Create a txn using either a TransactionDB or OptimisticTransactionDB
final Transaction txn = txnDb.beginTransaction(writeOptions);
txn.setSnapshot();

readOptions.setSnapshot(txnDb.getSnapshot());
byte[] value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);

...

value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);
txnDb.releaseSnapshot(readOptions.snapshot());
```

> 注意，在`ReadOptions`設定一個快照只會影響讀出來的數據的版本。他不會影響事務是否可以被提交。

如果你已經調用了SetSnapshot()，你也可以使用在事務裡設定的同一個快照。

```java
readOptions.setSnapshot(txnDb.getSnapshot());

value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);
```

## Tuning / Memory Usage

在內部，事務需要追踪那些key最近被修改過。現有的內存寫buffer會因此被重用。當決定內存中保留多少寫buffer的時候，事務仍舊遵從已有的**max_write_buffer_number**選項。另外，使用事務不影響落盤和壓縮。

切換到使用`[Optimistic]TransactionDB`可能會使用更多的內存。如果你曾經給**max_write_buffer_number**設置一個非常大的值，一個標準的RocksDB實例永遠都不會逼近這個最大內存限制。然而，一個`[Optimistic]TransactionDB`會嘗試使用盡可能多的寫buffer。這個可以通過減小max_write_buffer_number或者設置max_write_buffer_number_to_maintain為一個小於max_write_buffer_number的值來進行調優。

OptimisticTransactionDB：在提交的時候，樂觀事務會使用內存寫buffer來做衝突檢測。為此，緩存的數據必須比事務中修改的內容舊。否則，Commit會失敗。增加max_write_buffer_number_to_maintain以減小由於緩衝區不足導致的提交失敗。

TransactionDB：如果使用了SetSnapshot，Put/Delete/Merge/GetForUpdate操作會先檢查內存的緩衝區來做衝突檢測。如果沒有足夠的歷史數據在緩衝區，那麼會檢查SST文件。增加max_write_buffer_number_to_maintain會減少衝突檢測過程中的SST文件的讀操作。

## Save Points

除了`Rollback()`，事務還可以通過SavePoint來進行部分回滾。

```java
// Start a transaction
final Transaction txn = txnDb.beginTransaction(writeOptions);
txn.put("A".getBytes(UTF_8), "a".getBytes(UTF_8));
txn.setSavePoint();

txn.put("B".getBytes(UTF_8), "b".getBytes(UTF_8));
txn.rollbackToSavePoint();

// Since RollbackToSavePoint() was called, this transaction will only write 
// key A and not write key B.
txn.commit();
```

對資料庫併發控制原理有興趣的人可繼續看下一篇:[資料庫併發控制原理](lab3-ext_zh-tw.md)。

返回主目錄 >>  [README](README_zh-tw.md)


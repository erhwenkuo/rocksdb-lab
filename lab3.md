# Lab 3 - RocksDB Transaction

[Chinses version](lab3_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab3/) **

RocksDB supports Transactions when using a `TransactionDB `or `OptimisticTransactionDB`. Transactions have a simple **BEGIN**/**COMMIT**/**ROLLBACK** api and allow applications to modify their data concurrently while letting RocksDB handle the conflict checking. RocksDB supports both pessimistic and optimistic concurrency control.

Note that RocksDB provides Atomicity by default when writing multiple keys via `WriteBatch`. Transactions provide a way to guarantee that a batch of writes will only be written if there are no conflicts. Similar to a WriteBatch, no other threads can see the changes in a transaction until it has been written (committed).

A TransactionDB can be better for workloads with heavy concurrency compared to an OptimisticTransactionDB. However, there is a small cost to using a TransactionDB due to the **locking overhead**. A TransactionDB will do conflict checking for all write operations, including writes performed outside of a Transaction.

Locking timeouts and limits can be tuned in the TransactionDBOptions.

Below example shows how to use `TransactionDB` with **BEGIN**/**COMMIT**/**ROLLBACK** operations:

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

## OptimisticTransactionDB

Optimistic Transactions provide light-weight optimistic concurrency control for workloads that do not expect high contention/interference between multiple transactions.

Optimistic Transactions do not take any locks when preparing writes. Instead, they rely on doing conflict-detection at commit time to validate that no other writers have modified the keys being written by the current transaction. If there is a conflict with another write (or it cannot be determined), the commit will return an error and no keys will be written.

Optimistic concurrency control is useful for many workloads that need to protect against occasional write conflicts. However, this may not be a good solution for workloads where write-conflicts occur frequently due to many transactions constantly attempting to update the same keys. For these workloads, using a TransactionDB may be a better fit. An OptimisticTransactionDB may be more performant than a TransactionDB for workloads that have many non-transactional writes and few transactions.

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

Transactions also support easily reading the state of keys that are currently batched in a given transaction but not yet committed:

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

You should see the result like:

```sh
key:a, value:new
key:b, value:old
```

You can also iterate through keys that exist in both the db and the current transaction by using Transaction::GetIterator().


## Guarding against Read-Write Conflicts:

Call GetForUpdate() to read a key and make the read value a precondition for transaction commit.

```java
// Start a transaction
final Transaction txn = txnDb.beginTransaction(writeOptions);

// Read key1 in this transaction
byte[] value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8),true);

// Write to key1 OUTSIDE of the transaction
txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));
```

If this transaction was created by a `TransactionDB`, the `Put` would either _timeout_ or _block_ until the transaction commits or aborts. 

If this transaction were created by an `OptimisticTransactionDB()`, then the `Put` would succeed, but the transaction would not succeed if txn->Commit() were called.

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

Currently, GetForUpdate() is the only way to establish Read-Write conflicts, so it can be used in combination with iterators, for example.

## Setting a Snapshot

By default, Transaction conflict checking validates that no one else has written a key after the time the key was first written in this transaction. This isolation guarantee is sufficient for many use-cases. However, you may want to guarantee that no else has written a key since the start of the transaction. This can be accomplished by calling SetSnapshot() after creating the transaction.

Default behavior:

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

Using SetSnapshot():

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

> Note that in the `SetSnapshot()` case of the previous example, if this were a **TransactionDB**, the `Put()` would have failed. If this were an **OptimisticTransactionDB**, the `Commit()` would fail.

## Repeatable Read

Similar to normal RocksDB DB reads, you can achieve repeatable reads when reading through a transaction by setting a Snapshot in the ReadOptions.

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

> Note that Setting a snapshot in the ReadOptions only affects the version of the data that is read. This does not have any affect on whether the transaction will be able to be committed.

If you have called SetSnapshot(), you can read using the same snapshot that was set in the transaction:

```java
readOptions.setSnapshot(txnDb.getSnapshot());

value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);
```

## Tuning / Memory Usage

Internally, Transactions need to keep track of which keys have been written recently. The existing in-memory write buffers are re-used for this purpose. Transactions will still obey the existing **max_write_buffer_number** option when deciding how many write buffers to keep in memory. In addition, using transactions will not affect flushes or compactions.

It is possible that switching to using a `[Optimistic]TransactionDB` will use more memory than was used previously. If you have set a very large value for **max_write_buffer_number**, a typical RocksDB instance will could never come close to this maximum memory limit. However, an `[Optimistic]TransactionDB` will try to use as many write buffers as allowed. But this can be tuned by either reducing **max_write_buffer_number** or by setting **max_write_buffer_size_to_maintain**. See [memtable](https://github.com/facebook/rocksdb/wiki/MemTable) for more details about **max_write_buffer_size_to_maintain**.

## Save Points

In addition to Rollback(), Transactions can also be partially rolled back if `SavePoints` are used.

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

Back to main menu >>  [README](README.md)

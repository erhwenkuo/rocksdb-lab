package io.github.erhwenkuo.rocksdb.lab3;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *  Repeatable Read
 */
public class RepeatableRead {
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

        // Create a txn using either a TransactionDB or OptimisticTransactionDB
        final Transaction txn = txnDb.beginTransaction(writeOptions);
        txn.setSnapshot();

        try {
            readOptions.setSnapshot(txnDb.getSnapshot());
            byte[] value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);
            value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8), true);
            txnDb.releaseSnapshot(readOptions.snapshot());
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

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }
}

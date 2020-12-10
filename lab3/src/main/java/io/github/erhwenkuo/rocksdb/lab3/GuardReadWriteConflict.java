package io.github.erhwenkuo.rocksdb.lab3;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *  Guarding against Read-Write Conflicts
 */
public class GuardReadWriteConflict {
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

        // Start a transaction
        final Transaction txn = txnDb.beginTransaction(writeOptions);
        try {
            // Read key1 in this transaction
            byte[] value = txn.get(readOptions, "key1".getBytes(UTF_8));

            // Write to key1 OUTSIDE of the transaction
            txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));

            // No conflict since transactions only do conflict checking for keys read using GetForUpdate().
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

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }
}

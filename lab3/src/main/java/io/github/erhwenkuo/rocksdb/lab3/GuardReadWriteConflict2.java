package io.github.erhwenkuo.rocksdb.lab3;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GuardReadWriteConflict2 {
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
            // Read key1 in this transaction
            byte[] value = txn.getForUpdate(readOptions, "key1".getBytes(UTF_8),true);

            // Write to key1 OUTSIDE of the transaction
            txnDb.put(writeOptions, "key1".getBytes(UTF_8), "value0".getBytes(UTF_8));

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

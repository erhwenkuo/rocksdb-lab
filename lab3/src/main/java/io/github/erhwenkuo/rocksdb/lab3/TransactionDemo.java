package io.github.erhwenkuo.rocksdb.lab3;

import org.apache.commons.io.FileUtils;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *  Use `TransactionDB` with BEGIN/COMMIT/ROLLBACK operations
 */
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

        // remove RocksDB data directory, this is only for this lab
        FileUtils.deleteDirectory(new File(dbPath));
    }
}

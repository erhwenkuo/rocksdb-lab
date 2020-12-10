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
 *  Reading from a Transaction
 */
public class ReadingFromTransaction {
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

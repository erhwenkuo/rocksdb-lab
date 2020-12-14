package io.github.erhwenkuo.rocksdb.lab4;

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

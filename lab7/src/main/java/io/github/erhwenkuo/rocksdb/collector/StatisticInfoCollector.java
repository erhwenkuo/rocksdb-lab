package io.github.erhwenkuo.rocksdb.collector;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.SummaryMetricFamily;
import org.rocksdb.*;
import java.util.*;

public class StatisticInfoCollector extends Collector {
    final Statistics stats;

    final Map<TickerType, String> tickersNameMap = new HashMap<>();
    final Map<TickerType, String> tickersHelpMap = new HashMap<>();
    final Map<TickerType, String> countersNameMap = new HashMap<>();

    final Map<HistogramType, String> histogramsNameMap = new HashMap<>();
    final Map<HistogramType, String> histogramsHelpMap = new HashMap<>();
    final Map<HistogramType, String> histograms2NameMap = new HashMap<>();

    public StatisticInfoCollector(Statistics stats) {
        this.stats = stats;
        initTickerNameMap();
        initTickersHelpMap();
        initCounterNameMap();

        initHistogramsNameMap();
        initHistogramsHelpMap();
        initHistograms2NameMap();
    }

    // a map of "TickerType" to origin RocksDB Ticker name
    private void initTickerNameMap() {
        tickersNameMap.put(TickerType.BLOCK_CACHE_MISS, "rocksdb.block.cache.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_HIT, "rocksdb.block.cache.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_ADD, "rocksdb.block.cache.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_ADD_FAILURES, "rocksdb.block.cache.add.failures");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_MISS, "rocksdb.block.cache.index.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_HIT, "rocksdb.block.cache.index.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_ADD, "rocksdb.block.cache.index.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_BYTES_INSERT, "rocksdb.block.cache.index.bytes.insert");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_BYTES_EVICT, "rocksdb.block.cache.index.bytes.evict");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_MISS, "rocksdb.block.cache.filter.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_HIT, "rocksdb.block.cache.filter.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_ADD, "rocksdb.block.cache.filter.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_BYTES_INSERT, "rocksdb.block.cache.filter.bytes.insert");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_BYTES_EVICT, "rocksdb.block.cache.filter.bytes.evict");
        tickersNameMap.put(TickerType.BLOCK_CACHE_DATA_MISS, "rocksdb.block.cache.data.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_DATA_HIT, "rocksdb.block.cache.data.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_DATA_ADD, "rocksdb.block.cache.data.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_DATA_BYTES_INSERT, "rocksdb.block.cache.data.bytes.insert");
        tickersNameMap.put(TickerType.BLOCK_CACHE_BYTES_READ, "rocksdb.block.cache.bytes.read");
        tickersNameMap.put(TickerType.BLOCK_CACHE_BYTES_WRITE, "rocksdb.block.cache.bytes.write");
        tickersNameMap.put(TickerType.BLOOM_FILTER_USEFUL, "rocksdb.bloom.filter.useful");
        tickersNameMap.put(TickerType.BLOOM_FILTER_FULL_POSITIVE, "rocksdb.bloom.filter.full.positive");
        tickersNameMap.put(TickerType.BLOOM_FILTER_FULL_TRUE_POSITIVE, "rocksdb.bloom.filter.full.true.positive");
        //tickersNameMap.put(TickerType.BLOOM_FILTER_MICROS, "rocksdb.bloom.filter.micros");
        tickersNameMap.put(TickerType.PERSISTENT_CACHE_HIT, "rocksdb.persistent.cache.hit");
        tickersNameMap.put(TickerType.PERSISTENT_CACHE_MISS, "rocksdb.persistent.cache.miss");
        tickersNameMap.put(TickerType.SIM_BLOCK_CACHE_HIT, "rocksdb.sim.block.cache.hit");
        tickersNameMap.put(TickerType.SIM_BLOCK_CACHE_MISS, "rocksdb.sim.block.cache.miss");
        tickersNameMap.put(TickerType.MEMTABLE_HIT, "rocksdb.memtable.hit");
        tickersNameMap.put(TickerType.MEMTABLE_MISS, "rocksdb.memtable.miss");
        tickersNameMap.put(TickerType.GET_HIT_L0, "rocksdb.l0.hit");
        tickersNameMap.put(TickerType.GET_HIT_L1, "rocksdb.l1.hit");
        tickersNameMap.put(TickerType.GET_HIT_L2_AND_UP, "rocksdb.l2andup.hit");
        tickersNameMap.put(TickerType.COMPACTION_KEY_DROP_NEWER_ENTRY, "rocksdb.compaction.key.drop.new");
        tickersNameMap.put(TickerType.COMPACTION_KEY_DROP_OBSOLETE, "rocksdb.compaction.key.drop.obsolete");
        tickersNameMap.put(TickerType.COMPACTION_KEY_DROP_RANGE_DEL, "rocksdb.compaction.key.drop.range_del");
        tickersNameMap.put(TickerType.COMPACTION_KEY_DROP_USER, "rocksdb.compaction.key.drop.user");
        tickersNameMap.put(TickerType.COMPACTION_RANGE_DEL_DROP_OBSOLETE, "rocksdb.compaction.range_del.drop.obsolete");
        tickersNameMap.put(TickerType.COMPACTION_OPTIMIZED_DEL_DROP_OBSOLETE, "rocksdb.compaction.optimized.del.drop.obsolete");
        tickersNameMap.put(TickerType.COMPACTION_CANCELLED, "rocksdb.compaction.cancelled");
        tickersNameMap.put(TickerType.NUMBER_KEYS_WRITTEN, "rocksdb.number.keys.written");
        tickersNameMap.put(TickerType.NUMBER_KEYS_READ, "rocksdb.number.keys.read");
        tickersNameMap.put(TickerType.NUMBER_KEYS_UPDATED, "rocksdb.number.keys.updated");
        tickersNameMap.put(TickerType.BYTES_WRITTEN, "rocksdb.bytes.written");
        tickersNameMap.put(TickerType.BYTES_READ, "rocksdb.bytes.read");
        tickersNameMap.put(TickerType.NUMBER_DB_SEEK, "rocksdb.number.db.seek");
        tickersNameMap.put(TickerType.NUMBER_DB_NEXT, "rocksdb.number.db.next");
        tickersNameMap.put(TickerType.NUMBER_DB_PREV, "rocksdb.number.db.prev");
        tickersNameMap.put(TickerType.NUMBER_DB_SEEK_FOUND, "rocksdb.number.db.seek.found");
        tickersNameMap.put(TickerType.NUMBER_DB_NEXT_FOUND, "rocksdb.number.db.next.found");
        tickersNameMap.put(TickerType.NUMBER_DB_PREV_FOUND, "rocksdb.number.db.prev.found");
        tickersNameMap.put(TickerType.ITER_BYTES_READ, "rocksdb.db.iter.bytes.read");
        tickersNameMap.put(TickerType.NO_FILE_CLOSES, "rocksdb.no.file.closes");
        tickersNameMap.put(TickerType.NO_FILE_OPENS, "rocksdb.no.file.opens");
        tickersNameMap.put(TickerType.NO_FILE_ERRORS, "rocksdb.no.file.errors");
        tickersNameMap.put(TickerType.STALL_L0_SLOWDOWN_MICROS, "rocksdb.l0.slowdown.micros");
        tickersNameMap.put(TickerType.STALL_MEMTABLE_COMPACTION_MICROS, "rocksdb.memtable.compaction.micros");
        tickersNameMap.put(TickerType.STALL_L0_NUM_FILES_MICROS, "rocksdb.l0.num.files.stall.micros");
        tickersNameMap.put(TickerType.STALL_MICROS, "rocksdb.stall.micros");
        tickersNameMap.put(TickerType.DB_MUTEX_WAIT_MICROS, "rocksdb.db.mutex.wait.micros");
        tickersNameMap.put(TickerType.RATE_LIMIT_DELAY_MILLIS, "rocksdb.rate.limit.delay.millis");
        tickersNameMap.put(TickerType.NO_ITERATORS, "rocksdb.num.iterators");
        tickersNameMap.put(TickerType.NUMBER_MULTIGET_CALLS, "rocksdb.number.multiget.get");
        tickersNameMap.put(TickerType.NUMBER_MULTIGET_KEYS_READ, "rocksdb.number.multiget.keys.read");
        tickersNameMap.put(TickerType.NUMBER_MULTIGET_BYTES_READ, "rocksdb.number.multiget.bytes.read");
        tickersNameMap.put(TickerType.NUMBER_FILTERED_DELETES, "rocksdb.number.deletes.filtered");
        tickersNameMap.put(TickerType.NUMBER_MERGE_FAILURES, "rocksdb.number.merge.failures");
        tickersNameMap.put(TickerType.BLOOM_FILTER_PREFIX_CHECKED, "rocksdb.bloom.filter.prefix.checked");
        tickersNameMap.put(TickerType.BLOOM_FILTER_PREFIX_USEFUL, "rocksdb.bloom.filter.prefix.useful");
        tickersNameMap.put(TickerType.NUMBER_OF_RESEEKS_IN_ITERATION, "rocksdb.number.reseeks.iteration");
        tickersNameMap.put(TickerType.GET_UPDATES_SINCE_CALLS, "rocksdb.getupdatessince.calls");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSED_MISS, "rocksdb.block.cachecompressed.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSED_HIT, "rocksdb.block.cachecompressed.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSED_ADD, "rocksdb.block.cachecompressed.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSED_ADD_FAILURES, "rocksdb.block.cachecompressed.add.failures");
        tickersNameMap.put(TickerType.WAL_FILE_SYNCED, "rocksdb.wal.synced");
        tickersNameMap.put(TickerType.WAL_FILE_BYTES, "rocksdb.wal.bytes");
        tickersNameMap.put(TickerType.WRITE_DONE_BY_SELF, "rocksdb.write.self");
        tickersNameMap.put(TickerType.WRITE_DONE_BY_OTHER, "rocksdb.write.other");
        tickersNameMap.put(TickerType.WRITE_TIMEDOUT, "rocksdb.write.timeout");
        tickersNameMap.put(TickerType.WRITE_WITH_WAL, "rocksdb.write.wal");
        tickersNameMap.put(TickerType.COMPACT_READ_BYTES, "rocksdb.compact.read.bytes");
        tickersNameMap.put(TickerType.COMPACT_WRITE_BYTES, "rocksdb.compact.write.bytes");
        tickersNameMap.put(TickerType.FLUSH_WRITE_BYTES, "rocksdb.flush.write.bytes");
        tickersNameMap.put(TickerType.COMPACT_READ_BYTES_MARKED, "rocksdb.compact.read.marked.bytes");
        tickersNameMap.put(TickerType.COMPACT_READ_BYTES_PERIODIC, "rocksdb.compact.read.periodic.bytes");
        tickersNameMap.put(TickerType.COMPACT_READ_BYTES_TTL, "rocksdb.compact.read.ttl.bytes");
        tickersNameMap.put(TickerType.COMPACT_WRITE_BYTES_MARKED, "rocksdb.compact.write.marked.bytes");
        tickersNameMap.put(TickerType.COMPACT_WRITE_BYTES_PERIODIC, "rocksdb.compact.write.periodic.bytes");
        tickersNameMap.put(TickerType.COMPACT_WRITE_BYTES_TTL, "rocksdb.compact.write.ttl.bytes");
        tickersNameMap.put(TickerType.NUMBER_DIRECT_LOAD_TABLE_PROPERTIES, "rocksdb.number.direct.load.table.properties");
        tickersNameMap.put(TickerType.NUMBER_SUPERVERSION_ACQUIRES, "rocksdb.number.superversion_acquires");
        tickersNameMap.put(TickerType.NUMBER_SUPERVERSION_RELEASES, "rocksdb.number.superversion_releases");
        tickersNameMap.put(TickerType.NUMBER_SUPERVERSION_CLEANUPS, "rocksdb.number.superversion_cleanups");
        tickersNameMap.put(TickerType.NUMBER_BLOCK_COMPRESSED, "rocksdb.number.block.compressed");
        tickersNameMap.put(TickerType.NUMBER_BLOCK_DECOMPRESSED, "rocksdb.number.block.decompressed");
        tickersNameMap.put(TickerType.NUMBER_BLOCK_NOT_COMPRESSED, "rocksdb.number.block.not_compressed");
        tickersNameMap.put(TickerType.MERGE_OPERATION_TOTAL_TIME, "rocksdb.merge.operation.time.nanos");
        tickersNameMap.put(TickerType.FILTER_OPERATION_TOTAL_TIME, "rocksdb.filter.operation.time.nanos");
        tickersNameMap.put(TickerType.ROW_CACHE_HIT, "rocksdb.row.cache.hit");
        tickersNameMap.put(TickerType.ROW_CACHE_MISS, "rocksdb.row.cache.miss");
        tickersNameMap.put(TickerType.READ_AMP_ESTIMATE_USEFUL_BYTES, "rocksdb.read.amp.estimate.useful.bytes");
        tickersNameMap.put(TickerType.READ_AMP_TOTAL_READ_BYTES, "rocksdb.read.amp.total.read.bytes");
        tickersNameMap.put(TickerType.NUMBER_RATE_LIMITER_DRAINS, "rocksdb.number.rate_limiter.drains");
        tickersNameMap.put(TickerType.NUMBER_ITER_SKIP, "rocksdb.number.iter.skip");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_PUT, "rocksdb.blobdb.num.put");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_WRITE, "rocksdb.blobdb.num.write");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_GET, "rocksdb.blobdb.num.get");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_MULTIGET, "rocksdb.blobdb.num.multiget");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_SEEK, "rocksdb.blobdb.num.seek");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_NEXT, "rocksdb.blobdb.num.next");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_PREV, "rocksdb.blobdb.num.prev");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_KEYS_WRITTEN, "rocksdb.blobdb.num.keys.written");
        tickersNameMap.put(TickerType.BLOB_DB_NUM_KEYS_READ, "rocksdb.blobdb.num.keys.read");
        tickersNameMap.put(TickerType.BLOB_DB_BYTES_WRITTEN, "rocksdb.blobdb.bytes.written");
        tickersNameMap.put(TickerType.BLOB_DB_BYTES_READ, "rocksdb.blobdb.bytes.read");
        tickersNameMap.put(TickerType.BLOB_DB_WRITE_INLINED, "rocksdb.blobdb.write.inlined");
        tickersNameMap.put(TickerType.BLOB_DB_WRITE_INLINED_TTL, "rocksdb.blobdb.write.inlined.ttl");
        tickersNameMap.put(TickerType.BLOB_DB_WRITE_BLOB, "rocksdb.blobdb.write.blob");
        tickersNameMap.put(TickerType.BLOB_DB_WRITE_BLOB_TTL, "rocksdb.blobdb.write.blob.ttl");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_FILE_BYTES_WRITTEN, "rocksdb.blobdb.blob.file.bytes.written");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_FILE_BYTES_READ, "rocksdb.blobdb.blob.file.bytes.read");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_FILE_SYNCED, "rocksdb.blobdb.blob.file.synced");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_COUNT, "rocksdb.blobdb.blob.index.expired.count");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_SIZE, "rocksdb.blobdb.blob.index.expired.size");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_INDEX_EVICTED_COUNT, "rocksdb.blobdb.blob.index.evicted.count");
        tickersNameMap.put(TickerType.BLOB_DB_BLOB_INDEX_EVICTED_SIZE, "rocksdb.blobdb.blob.index.evicted.size");
        tickersNameMap.put(TickerType.BLOB_DB_GC_NUM_FILES, "rocksdb.blobdb.gc.num.files");
        tickersNameMap.put(TickerType.BLOB_DB_GC_NUM_NEW_FILES, "rocksdb.blobdb.gc.num.new.files");
        tickersNameMap.put(TickerType.BLOB_DB_GC_FAILURES, "rocksdb.blobdb.gc.failures");
        tickersNameMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_OVERWRITTEN, "rocksdb.blobdb.gc.num.keys.overwritten");
        tickersNameMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_EXPIRED, "rocksdb.blobdb.gc.num.keys.expired");
        tickersNameMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_RELOCATED, "rocksdb.blobdb.gc.num.keys.relocated");
        tickersNameMap.put(TickerType.BLOB_DB_GC_BYTES_OVERWRITTEN, "rocksdb.blobdb.gc.bytes.overwritten");
        tickersNameMap.put(TickerType.BLOB_DB_GC_BYTES_EXPIRED, "rocksdb.blobdb.gc.bytes.expired");
        tickersNameMap.put(TickerType.BLOB_DB_GC_BYTES_RELOCATED, "rocksdb.blobdb.gc.bytes.relocated");
        tickersNameMap.put(TickerType.BLOB_DB_FIFO_NUM_FILES_EVICTED, "rocksdb.blobdb.fifo.num.files.evicted");
        tickersNameMap.put(TickerType.BLOB_DB_FIFO_NUM_KEYS_EVICTED, "rocksdb.blobdb.fifo.num.keys.evicted");
        tickersNameMap.put(TickerType.BLOB_DB_FIFO_BYTES_EVICTED, "rocksdb.blobdb.fifo.bytes.evicted");
        tickersNameMap.put(TickerType.TXN_PREPARE_MUTEX_OVERHEAD, "rocksdb.txn.overhead.mutex.prepare");
        tickersNameMap.put(TickerType.TXN_OLD_COMMIT_MAP_MUTEX_OVERHEAD, "rocksdb.txn.overhead.mutex.old.commit.map");
        tickersNameMap.put(TickerType.TXN_DUPLICATE_KEY_OVERHEAD, "rocksdb.txn.overhead.duplicate.key");
        tickersNameMap.put(TickerType.TXN_SNAPSHOT_MUTEX_OVERHEAD, "rocksdb.txn.overhead.mutex.snapshot");
        tickersNameMap.put(TickerType.TXN_GET_TRY_AGAIN, "rocksdb.txn.get.tryagain");
        tickersNameMap.put(TickerType.NUMBER_MULTIGET_KEYS_FOUND, "rocksdb.number.multiget.keys.found");
        tickersNameMap.put(TickerType.NO_ITERATOR_CREATED, "rocksdb.num.iterator.created");
        tickersNameMap.put(TickerType.NO_ITERATOR_DELETED, "rocksdb.num.iterator.deleted");
        /*
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_MISS, "rocksdb.block.cache.compression.dict.miss");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_HIT, "rocksdb.block.cache.compression.dict.hit");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD, "rocksdb.block.cache.compression.dict.add");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_BYTES_INSERT, "rocksdb.block.cache.compression.dict.bytes.insert");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_BYTES_EVICT, "rocksdb.block.cache.compression.dict.bytes.evict");
        tickersNameMap.put(TickerType.BLOCK_CACHE_ADD_REDUNDANT, "rocksdb.block.cache.add.redundant");
        tickersNameMap.put(TickerType.BLOCK_CACHE_INDEX_ADD_REDUNDANT,"rocksdb.block.cache.index.add.redundant");
        tickersNameMap.put(TickerType.BLOCK_CACHE_FILTER_ADD_REDUNDANT, "rocksdb.block.cache.filter.add.redundant");
        tickersNameMap.put(TickerType.BLOCK_CACHE_DATA_ADD_REDUNDANT, "rocksdb.block.cache.data.add.redundant");
        tickersNameMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD_REDUNDANT, "rocksdb.block.cache.compression.dict.add.redundant");
         */
        tickersNameMap.put(TickerType.FILES_MARKED_TRASH, "rocksdb.files.marked.trash");
        tickersNameMap.put(TickerType.FILES_DELETED_IMMEDIATELY, "rocksdb.files.deleted.immediately");
    }

    // a map of "TickerType" to origin RocksDB Ticker description
    private void initTickersHelpMap() {
        tickersHelpMap.put(TickerType.BLOCK_CACHE_MISS, "Number of times total block cache misses.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_HIT, "Number of times total block cache hit.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_ADD, "Number of times block added to block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_ADD_FAILURES, "Number of failures when adding blocks to block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_MISS, "Number of times cache miss when accessing index block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_HIT, "Number of times cache hit when accessing index block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_ADD, "Number of index blocks added to block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_BYTES_INSERT, "Number of bytes of index blocks inserted into cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_BYTES_EVICT, "Number of bytes of index block erased from cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_MISS, "Number of times cache miss when accessing filter block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_HIT, "Number of times cache hit when accessing filter block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_ADD, "Number of filter blocks added to block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_BYTES_INSERT, "Number of bytes of bloom filter blocks inserted into cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_BYTES_EVICT, "Number of bytes of bloom filter block erased from cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_DATA_MISS, "Number of times cache miss when accessing data block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_DATA_HIT, "Number of times cache hit when accessing data block from block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_DATA_ADD, "Number of data blocks added to block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_DATA_BYTES_INSERT, "Number of bytes of data blocks inserted into cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_BYTES_READ, "Number of bytes read from cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_BYTES_WRITE, "Number of bytes written into cache.");
        tickersHelpMap.put(TickerType.BLOOM_FILTER_USEFUL, "Number of times bloom filter has avoided file reads.");
        tickersHelpMap.put(TickerType.BLOOM_FILTER_FULL_POSITIVE, "Number of times bloom FullFilter has not avoided the reads.");
        tickersHelpMap.put(TickerType.BLOOM_FILTER_FULL_TRUE_POSITIVE, "Number of times bloom FullFilter has not avoided the reads and data actually exist.");
        //tickersHelpMap.put(TickerType.BLOOM_FILTER_MICROS, "rocksdb.bloom.filter.micros");
        tickersHelpMap.put(TickerType.PERSISTENT_CACHE_HIT, "Number persistent cache hit.");
        tickersHelpMap.put(TickerType.PERSISTENT_CACHE_MISS, "Number persistent cache miss.");
        tickersHelpMap.put(TickerType.SIM_BLOCK_CACHE_HIT, "Number total simulation block cache hits.");
        tickersHelpMap.put(TickerType.SIM_BLOCK_CACHE_MISS, "Number total simulation block cache misses.");
        tickersHelpMap.put(TickerType.MEMTABLE_HIT, "Number of memtable hits.");
        tickersHelpMap.put(TickerType.MEMTABLE_MISS, "Number of memtable misses.");
        tickersHelpMap.put(TickerType.GET_HIT_L0, "Number of Get() queries served by L0.");
        tickersHelpMap.put(TickerType.GET_HIT_L1, "Number of Get() queries served by L1.");
        tickersHelpMap.put(TickerType.GET_HIT_L2_AND_UP, "Number of Get() queries served by L2 and up.");
        tickersHelpMap.put(TickerType.COMPACTION_KEY_DROP_NEWER_ENTRY, "Key was written with a newer value.");
        tickersHelpMap.put(TickerType.COMPACTION_KEY_DROP_OBSOLETE, "Also includes keys dropped for range del. The key is obsolete.");
        tickersHelpMap.put(TickerType.COMPACTION_KEY_DROP_RANGE_DEL, "The key was covered by a range tombstone.");
        tickersHelpMap.put(TickerType.COMPACTION_KEY_DROP_USER, "User compaction function has dropped the key.");
        tickersHelpMap.put(TickerType.COMPACTION_RANGE_DEL_DROP_OBSOLETE, "All keys in range were deleted.");
        tickersHelpMap.put(TickerType.COMPACTION_OPTIMIZED_DEL_DROP_OBSOLETE, "Deletions obsoleted before bottom level due to file gap optimization.");
        tickersHelpMap.put(TickerType.COMPACTION_CANCELLED, "Number of compaction cancelled in sfm to prevent ENOSPC.");
        tickersHelpMap.put(TickerType.NUMBER_KEYS_WRITTEN, "Number of keys written to the database via the Put and Write call's.");
        tickersHelpMap.put(TickerType.NUMBER_KEYS_READ, "Number of Keys read.");
        tickersHelpMap.put(TickerType.NUMBER_KEYS_UPDATED, "Number keys updated, if inplace update is enabled.");
        tickersHelpMap.put(TickerType.BYTES_WRITTEN, "Number of uncompressed bytes issued by DB::Put(), DB::Delete(), DB::Merge(), and DB::Write().");
        tickersHelpMap.put(TickerType.BYTES_READ, "Number of uncompressed bytes read from DB::Get().");
        tickersHelpMap.put(TickerType.NUMBER_DB_SEEK, "The number of calls to seek.");
        tickersHelpMap.put(TickerType.NUMBER_DB_NEXT, "The number of calls to next.");
        tickersHelpMap.put(TickerType.NUMBER_DB_PREV, "The number of calls to prev.");
        tickersHelpMap.put(TickerType.NUMBER_DB_SEEK_FOUND, "The number of calls to seek that returned data.");
        tickersHelpMap.put(TickerType.NUMBER_DB_NEXT_FOUND, "The number of calls to next that returned data.");
        tickersHelpMap.put(TickerType.NUMBER_DB_PREV_FOUND, "The number of calls to prev that returned data.");
        tickersHelpMap.put(TickerType.ITER_BYTES_READ, "The number of uncompressed bytes read from an iterator.");
        tickersHelpMap.put(TickerType.NO_FILE_CLOSES, "Number of file closes.");
        tickersHelpMap.put(TickerType.NO_FILE_OPENS, "Number of file opens.");
        tickersHelpMap.put(TickerType.NO_FILE_ERRORS, "Number of file errors.");
        tickersHelpMap.put(TickerType.STALL_L0_SLOWDOWN_MICROS, "Time system had to wait to do LO-L1 compactions.");
        tickersHelpMap.put(TickerType.STALL_MEMTABLE_COMPACTION_MICROS, "Time system had to wait to move memtable to L1.");
        tickersHelpMap.put(TickerType.STALL_L0_NUM_FILES_MICROS, "Write throttle because of too many files in L0.");
        tickersHelpMap.put(TickerType.STALL_MICROS, "Writer has to wait for compaction or flush to finish.");
        tickersHelpMap.put(TickerType.DB_MUTEX_WAIT_MICROS, "The wait time for db mutex.");
        tickersHelpMap.put(TickerType.RATE_LIMIT_DELAY_MILLIS, ""); // [rocksdb.rate.limit.delay.millis]
        tickersHelpMap.put(TickerType.NO_ITERATORS, "Number of iterators created.");
        tickersHelpMap.put(TickerType.NUMBER_MULTIGET_CALLS, "Number of MultiGet calls.");
        tickersHelpMap.put(TickerType.NUMBER_MULTIGET_KEYS_READ, "Number of MultiGet keys read.");
        tickersHelpMap.put(TickerType.NUMBER_MULTIGET_BYTES_READ, "Number of MultiGet bytes read.");
        tickersHelpMap.put(TickerType.NUMBER_FILTERED_DELETES, "Number of deletes records that were not required to be written to storage because key does not exist.");
        tickersHelpMap.put(TickerType.NUMBER_MERGE_FAILURES, "Number of merge records that were not required to be written to storage because key does not exist.");
        tickersHelpMap.put(TickerType.BLOOM_FILTER_PREFIX_CHECKED, "Number of times bloom was checked before creating iterator on a file.");
        tickersHelpMap.put(TickerType.BLOOM_FILTER_PREFIX_USEFUL, "Number of times the bloom check was useful in avoiding iterator creation.");
        tickersHelpMap.put(TickerType.NUMBER_OF_RESEEKS_IN_ITERATION, "Number of times we had to reseek inside an iteration to skip over large number of keys with same userkey.");
        tickersHelpMap.put(TickerType.GET_UPDATES_SINCE_CALLS, "Number of calls to RocksDB.getUpdatesSince(long).");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSED_MISS, "Number of times miss in the compressed block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSED_HIT, "Number of times hit in the compressed block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSED_ADD, "Number of blocks added to compressed block cache.");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSED_ADD_FAILURES, "Number of times failures occurred when adding blocks to compressed block cache.");
        tickersHelpMap.put(TickerType.WAL_FILE_SYNCED, "Number of times WAL sync is done.");
        tickersHelpMap.put(TickerType.WAL_FILE_BYTES, "Number of bytes written to WAL.");
        tickersHelpMap.put(TickerType.WRITE_DONE_BY_SELF, "Writes can be processed by requesting thread or by the thread at the head of the writers queue.");
        tickersHelpMap.put(TickerType.WRITE_DONE_BY_OTHER, "Equivalent to writes done for others.");
        tickersHelpMap.put(TickerType.WRITE_TIMEDOUT, "Number of writes ending up with timed-out.");
        tickersHelpMap.put(TickerType.WRITE_WITH_WAL, "Number of Write calls that request WAL.");
        tickersHelpMap.put(TickerType.COMPACT_READ_BYTES, "Bytes read during compaction.");
        tickersHelpMap.put(TickerType.COMPACT_WRITE_BYTES, "Bytes written during compaction.");
        tickersHelpMap.put(TickerType.FLUSH_WRITE_BYTES, "Number of bytes written during flush.");
        tickersHelpMap.put(TickerType.COMPACT_READ_BYTES_MARKED, "Number of bytes read for compaction due to marked operation.");
        tickersHelpMap.put(TickerType.COMPACT_READ_BYTES_PERIODIC, "Number of bytes read for compaction due to periodic operation.");
        tickersHelpMap.put(TickerType.COMPACT_READ_BYTES_TTL, "Number of bytes read for compaction due to ttl operation.");
        tickersHelpMap.put(TickerType.COMPACT_WRITE_BYTES_MARKED, "Number of bytes written for compaction due to marked operation.");
        tickersHelpMap.put(TickerType.COMPACT_WRITE_BYTES_PERIODIC, "Number of bytes written for compaction due to periodic operation.");
        tickersHelpMap.put(TickerType.COMPACT_WRITE_BYTES_TTL, "Number of bytes written for compaction due to ttl operation.");
        tickersHelpMap.put(TickerType.NUMBER_DIRECT_LOAD_TABLE_PROPERTIES, "Number of table's properties loaded directly from file, without creating table reader object.");
        tickersHelpMap.put(TickerType.NUMBER_SUPERVERSION_ACQUIRES, ""); // [rocksdb.number.superversion_acquires]
        tickersHelpMap.put(TickerType.NUMBER_SUPERVERSION_RELEASES, ""); // [rocksdb.number.superversion_releases]
        tickersHelpMap.put(TickerType.NUMBER_SUPERVERSION_CLEANUPS, ""); // [rocksdb.number.superversion_cleanups]
        tickersHelpMap.put(TickerType.NUMBER_BLOCK_COMPRESSED, "Number of compressions executed.");
        tickersHelpMap.put(TickerType.NUMBER_BLOCK_DECOMPRESSED, "Number of decompressions executed.");
        tickersHelpMap.put(TickerType.NUMBER_BLOCK_NOT_COMPRESSED, "Number of blocks not compressed.");
        tickersHelpMap.put(TickerType.MERGE_OPERATION_TOTAL_TIME, "Number of duration (in nano sec) spent for merge operation.");
        tickersHelpMap.put(TickerType.FILTER_OPERATION_TOTAL_TIME, "Number of duration (in nano sec) spent for filter operation.");
        tickersHelpMap.put(TickerType.ROW_CACHE_HIT, "Number of row cache hit.");
        tickersHelpMap.put(TickerType.ROW_CACHE_MISS, "Number of row cache miss.");
        tickersHelpMap.put(TickerType.READ_AMP_ESTIMATE_USEFUL_BYTES, "Estimate of total bytes actually used.");
        tickersHelpMap.put(TickerType.READ_AMP_TOTAL_READ_BYTES, "Total size of loaded data blocks.");
        tickersHelpMap.put(TickerType.NUMBER_RATE_LIMITER_DRAINS, "Number of refill intervals where rate limiter's bytes are fully consumed.");
        tickersHelpMap.put(TickerType.NUMBER_ITER_SKIP, "Number of internal skipped during iteration.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_PUT, "Number of Put/PutTTL/PutUntil to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_WRITE, "Number of Write to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_GET, "Number of Get to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_MULTIGET, "Number of MultiGet to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_SEEK, "Number of Seek/SeekToFirst/SeekToLast/SeekForPrev to BlobDB iterator.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_NEXT, "Number of Next to BlobDB iterator.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_PREV, "Number of Prev to BlobDB iterator.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_KEYS_WRITTEN, "Number of keys written to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_NUM_KEYS_READ, "Number of keys read from BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_BYTES_WRITTEN, "Number of bytes (key + value) written to BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_BYTES_READ, "Number of bytes (keys + value) read from BlobDB.");
        tickersHelpMap.put(TickerType.BLOB_DB_WRITE_INLINED, "Number of keys written by BlobDB as non-TTL inlined value.");
        tickersHelpMap.put(TickerType.BLOB_DB_WRITE_INLINED_TTL, "Number of keys written by BlobDB as TTL inlined value.");
        tickersHelpMap.put(TickerType.BLOB_DB_WRITE_BLOB, "Number of keys written by BlobDB as non-TTL blob value.");
        tickersHelpMap.put(TickerType.BLOB_DB_WRITE_BLOB_TTL, "Number of keys written by BlobDB as TTL blob value.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_FILE_BYTES_WRITTEN, "Number of bytes written to blob file.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_FILE_BYTES_READ, "Number of bytes read from blob file.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_FILE_SYNCED, "Number of times a blob files being synced.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_COUNT, "Number of blob index evicted from base DB by BlobDB compaction filter because of expiration.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_SIZE, "Size of blob index evicted from base DB by BlobDB compaction filter because of expiration.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_INDEX_EVICTED_COUNT, "Number of blob index evicted from base DB by BlobDB compaction filter because of corresponding file deleted.");
        tickersHelpMap.put(TickerType.BLOB_DB_BLOB_INDEX_EVICTED_SIZE, "Size of blob index evicted from base DB by BlobDB compaction filter because of corresponding file deleted.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_NUM_FILES, "Number of blob files being garbage collected.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_NUM_NEW_FILES, "Number of blob files generated by garbage collection.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_FAILURES, "Number of BlobDB garbage collection failures.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_OVERWRITTEN, "Number of keys drop by BlobDB garbage collection because they had been overwritten.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_EXPIRED, "Number of keys drop by BlobDB garbage collection because of expiration.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_NUM_KEYS_RELOCATED, "Number of keys relocated to new blob file by garbage collection.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_BYTES_OVERWRITTEN, "Number of bytes drop by BlobDB garbage collection because they had been overwritten.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_BYTES_EXPIRED, "Number of bytes drop by BlobDB garbage collection because of expiration.");
        tickersHelpMap.put(TickerType.BLOB_DB_GC_BYTES_RELOCATED, "Number of bytes relocated to new blob file by garbage collection.");
        tickersHelpMap.put(TickerType.BLOB_DB_FIFO_NUM_FILES_EVICTED, "Number of blob files evicted because of BlobDB is full.");
        tickersHelpMap.put(TickerType.BLOB_DB_FIFO_NUM_KEYS_EVICTED, "Number of keys in the blob files evicted because of BlobDB is full.");
        tickersHelpMap.put(TickerType.BLOB_DB_FIFO_BYTES_EVICTED, "Number of bytes in the blob files evicted because of BlobDB is full.");
        tickersHelpMap.put(TickerType.TXN_PREPARE_MUTEX_OVERHEAD, "Number of times prepare_mutex_ is acquired in the fast path.");
        tickersHelpMap.put(TickerType.TXN_OLD_COMMIT_MAP_MUTEX_OVERHEAD, "Number of times old_commit_map_mutex_ is acquired in the fast path.");
        tickersHelpMap.put(TickerType.TXN_DUPLICATE_KEY_OVERHEAD, "Number of times we checked a batch for duplicate keys.");
        tickersHelpMap.put(TickerType.TXN_SNAPSHOT_MUTEX_OVERHEAD, "Number of times snapshot_mutex_ is acquired in the fast path.");
        tickersHelpMap.put(TickerType.TXN_GET_TRY_AGAIN, "Number of times Get() returned TryAgain due to expired snapshot seq.");
        tickersHelpMap.put(TickerType.NUMBER_MULTIGET_KEYS_FOUND, "Number of MultiGet keys found (vs number requested).");
        tickersHelpMap.put(TickerType.NO_ITERATOR_CREATED, "Number of iterators created.");
        tickersHelpMap.put(TickerType.NO_ITERATOR_DELETED, "Number of iterators deleted.");
        /*
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_MISS, "rocksdb.block.cache.compression.dict.miss");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_HIT, "rocksdb.block.cache.compression.dict.hit");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD, "rocksdb.block.cache.compression.dict.add");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_BYTES_INSERT, "rocksdb.block.cache.compression.dict.bytes.insert");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_BYTES_EVICT, "rocksdb.block.cache.compression.dict.bytes.evict");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_ADD_REDUNDANT, "rocksdb.block.cache.add.redundant");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_INDEX_ADD_REDUNDANT,"rocksdb.block.cache.index.add.redundant");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_FILTER_ADD_REDUNDANT, "rocksdb.block.cache.filter.add.redundant");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_DATA_ADD_REDUNDANT, "rocksdb.block.cache.data.add.redundant");
        tickersHelpMap.put(TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD_REDUNDANT, "rocksdb.block.cache.compression.dict.add.redundant");
         */
        tickersHelpMap.put(TickerType.FILES_MARKED_TRASH, "Number of files marked as trash by delete scheduler.");
        tickersHelpMap.put(TickerType.FILES_DELETED_IMMEDIATELY, "Number of files deleted immediately by delete scheduler.");
    }

    // a map of "TickerType" to Prometheus Metric name
    private void initCounterNameMap() {
        for (TickerType tickerType : TickerType.values()) {
            if(tickerType != TickerType.TICKER_ENUM_MAX) {
                final String name = tickersNameMap.get(tickerType);
                final String counterName = name.replace('.', '_');
                countersNameMap.put(tickerType, counterName);
            }
        }
    }

    // a map of "HistogramType" to origin RocksDB HistogramName
    private void initHistogramsNameMap() {
        histogramsNameMap.put(HistogramType.DB_GET, "rocksdb.db.get.micros");
        histogramsNameMap.put(HistogramType.DB_WRITE, "rocksdb.db.write.micros");
        histogramsNameMap.put(HistogramType.COMPACTION_TIME, "rocksdb.compaction.times.micros");
        //histogramsNameMap.put(HistogramType.COMPACTION_CPU_TIME, "rocksdb.compaction.times.cpu_micros");
        histogramsNameMap.put(HistogramType.SUBCOMPACTION_SETUP_TIME, "rocksdb.subcompaction.setup.times.micros");
        histogramsNameMap.put(HistogramType.TABLE_SYNC_MICROS, "rocksdb.table.sync.micros");
        histogramsNameMap.put(HistogramType.COMPACTION_OUTFILE_SYNC_MICROS, "rocksdb.compaction.outfile.sync.micros");
        histogramsNameMap.put(HistogramType.WAL_FILE_SYNC_MICROS, "rocksdb.wal.file.sync.micros");
        histogramsNameMap.put(HistogramType.MANIFEST_FILE_SYNC_MICROS, "rocksdb.manifest.file.sync.micros");
        histogramsNameMap.put(HistogramType.TABLE_OPEN_IO_MICROS, "rocksdb.table.open.io.micros");
        histogramsNameMap.put(HistogramType.DB_MULTIGET, "rocksdb.db.multiget.micros");
        histogramsNameMap.put(HistogramType.READ_BLOCK_COMPACTION_MICROS, "rocksdb.read.block.compaction.micros");
        histogramsNameMap.put(HistogramType.READ_BLOCK_GET_MICROS, "rocksdb.read.block.get.micros");
        histogramsNameMap.put(HistogramType.WRITE_RAW_BLOCK_MICROS, "rocksdb.write.raw.block.micros");
        histogramsNameMap.put(HistogramType.STALL_L0_SLOWDOWN_COUNT, "rocksdb.l0.slowdown.count");
        histogramsNameMap.put(HistogramType.STALL_MEMTABLE_COMPACTION_COUNT, "rocksdb.memtable.compaction.count");
        histogramsNameMap.put(HistogramType.STALL_L0_NUM_FILES_COUNT, "rocksdb.num.files.stall.count");
        histogramsNameMap.put(HistogramType.HARD_RATE_LIMIT_DELAY_COUNT, "rocksdb.hard.rate.limit.delay.count");
        histogramsNameMap.put(HistogramType.SOFT_RATE_LIMIT_DELAY_COUNT, "rocksdb.soft.rate.limit.delay.count");
        histogramsNameMap.put(HistogramType.NUM_FILES_IN_SINGLE_COMPACTION, "rocksdb.numfiles.in.singlecompaction");
        histogramsNameMap.put(HistogramType.DB_SEEK, "rocksdb.db.seek.micros");
        histogramsNameMap.put(HistogramType.WRITE_STALL, "rocksdb.db.write.stall");
        histogramsNameMap.put(HistogramType.SST_READ_MICROS, "rocksdb.sst.read.micros");
        histogramsNameMap.put(HistogramType.NUM_SUBCOMPACTIONS_SCHEDULED, "rocksdb.num.subcompactions.scheduled");
        histogramsNameMap.put(HistogramType.BYTES_PER_READ, "rocksdb.bytes.per.read");
        histogramsNameMap.put(HistogramType.BYTES_PER_WRITE, "rocksdb.bytes.per.write");
        histogramsNameMap.put(HistogramType.BYTES_PER_MULTIGET, "rocksdb.bytes.per.multiget");
        histogramsNameMap.put(HistogramType.BYTES_COMPRESSED, "rocksdb.bytes.compressed");
        histogramsNameMap.put(HistogramType.BYTES_DECOMPRESSED, "rocksdb.bytes.decompressed");
        histogramsNameMap.put(HistogramType.COMPRESSION_TIMES_NANOS, "rocksdb.compression.times.nanos");
        histogramsNameMap.put(HistogramType.DECOMPRESSION_TIMES_NANOS, "rocksdb.decompression.times.nanos");
        histogramsNameMap.put(HistogramType.READ_NUM_MERGE_OPERANDS, "rocksdb.read.num.merge_operands");
        histogramsNameMap.put(HistogramType.BLOB_DB_KEY_SIZE, "rocksdb.blobdb.key.size");
        histogramsNameMap.put(HistogramType.BLOB_DB_VALUE_SIZE, "rocksdb.blobdb.value.size");
        histogramsNameMap.put(HistogramType.BLOB_DB_WRITE_MICROS, "rocksdb.blobdb.write.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_GET_MICROS, "rocksdb.blobdb.get.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_MULTIGET_MICROS, "rocksdb.blobdb.multiget.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_SEEK_MICROS, "rocksdb.blobdb.seek.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_NEXT_MICROS, "rocksdb.blobdb.next.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_PREV_MICROS, "rocksdb.blobdb.prev.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_BLOB_FILE_WRITE_MICROS, "rocksdb.blobdb.blob.file.write.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_BLOB_FILE_READ_MICROS, "rocksdb.blobdb.blob.file.read.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_BLOB_FILE_SYNC_MICROS, "rocksdb.blobdb.blob.file.sync.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_GC_MICROS, "rocksdb.blobdb.gc.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_COMPRESSION_MICROS, "rocksdb.blobdb.compression.micros");
        histogramsNameMap.put(HistogramType.BLOB_DB_DECOMPRESSION_MICROS, "rocksdb.blobdb.decompression.micros");
        histogramsNameMap.put(HistogramType.FLUSH_TIME, "rocksdb.db.flush.micros");
        /*
        histogramsNameMap.put(HistogramType.SST_BATCH_SIZE, "rocksdb.sst.batch.size");
        histogramsNameMap.put(HistogramType.NUM_INDEX_AND_FILTER_BLOCKS_READ_PER_LEVEL, "rocksdb.num.index.and.filter.blocks.read.per.level");
        histogramsNameMap.put(HistogramType.NUM_DATA_BLOCKS_READ_PER_LEVEL, "rocksdb.num.data.blocks.read.per.level");
        histogramsNameMap.put(HistogramType.NUM_SST_READ_PER_LEVEL, "rocksdb.num.sst.read.per.level");
        */
    }

    // a map of "HistogramType" to origin RocksDB Histogram description
    private void initHistogramsHelpMap() {
        histogramsHelpMap.put(HistogramType.DB_GET, "RocksDB Get latency.");
        histogramsHelpMap.put(HistogramType.DB_WRITE, "RocksDB Put/PutWithTTL/PutUntil/Write latency.");
        histogramsHelpMap.put(HistogramType.COMPACTION_TIME, "RocksDB compaction time.");
        //histogramsHelpMap.put(HistogramType.COMPACTION_CPU_TIME, ""); // [rocksdb.compaction.times.cpu_micros]
        histogramsHelpMap.put(HistogramType.SUBCOMPACTION_SETUP_TIME, ""); // [rocksdb.subcompaction.setup.times.micros]
        histogramsHelpMap.put(HistogramType.TABLE_SYNC_MICROS, ""); // [rocksdb.table.sync.micros]
        histogramsHelpMap.put(HistogramType.COMPACTION_OUTFILE_SYNC_MICROS, ""); // [rocksdb.compaction.outfile.sync.micros]
        histogramsHelpMap.put(HistogramType.WAL_FILE_SYNC_MICROS, ""); // [rocksdb.wal.file.sync.micros]
        histogramsHelpMap.put(HistogramType.MANIFEST_FILE_SYNC_MICROS, ""); // [rocksdb.manifest.file.sync.micros]
        histogramsHelpMap.put(HistogramType.TABLE_OPEN_IO_MICROS, "Time spent in IO during table open.");
        histogramsHelpMap.put(HistogramType.DB_MULTIGET, "RocksDB MultiGet latency.");
        histogramsHelpMap.put(HistogramType.READ_BLOCK_COMPACTION_MICROS, ""); // [rocksdb.read.block.compaction.micros]
        histogramsHelpMap.put(HistogramType.READ_BLOCK_GET_MICROS, ""); // [rocksdb.read.block.get.micros]
        histogramsHelpMap.put(HistogramType.WRITE_RAW_BLOCK_MICROS, ""); // [rocksdb.write.raw.block.micros]
        histogramsHelpMap.put(HistogramType.STALL_L0_SLOWDOWN_COUNT, ""); // [rocksdb.l0.slowdown.count]
        histogramsHelpMap.put(HistogramType.STALL_MEMTABLE_COMPACTION_COUNT, ""); // [rocksdb.memtable.compaction.count]
        histogramsHelpMap.put(HistogramType.STALL_L0_NUM_FILES_COUNT, "Write throttle because of too many files in L0.");
        histogramsHelpMap.put(HistogramType.HARD_RATE_LIMIT_DELAY_COUNT, ""); // [rocksdb.hard.rate.limit.delay.count]
        histogramsHelpMap.put(HistogramType.SOFT_RATE_LIMIT_DELAY_COUNT, ""); // [rocksdb.soft.rate.limit.delay.count]
        histogramsHelpMap.put(HistogramType.NUM_FILES_IN_SINGLE_COMPACTION, ""); // [rocksdb.numfiles.in.singlecompaction]
        histogramsHelpMap.put(HistogramType.DB_SEEK, "RocksDB Seek/SeekToFirst/SeekToLast/SeekForPrev latency.");
        histogramsHelpMap.put(HistogramType.WRITE_STALL, "Time spent that writer has to wait for compaction or flush to finish.");
        histogramsHelpMap.put(HistogramType.SST_READ_MICROS, ""); // [rocksdb.sst.read.micros]
        histogramsHelpMap.put(HistogramType.NUM_SUBCOMPACTIONS_SCHEDULED, "The number of subcompactions actually scheduled during a compaction.");
        histogramsHelpMap.put(HistogramType.BYTES_PER_READ, "Value size distribution of read operation.");
        histogramsHelpMap.put(HistogramType.BYTES_PER_WRITE, "Value size distribution of write operation.");
        histogramsHelpMap.put(HistogramType.BYTES_PER_MULTIGET, "Value size distribution of multiget operation.");
        histogramsHelpMap.put(HistogramType.BYTES_COMPRESSED, "Number of bytes compressed.");
        histogramsHelpMap.put(HistogramType.BYTES_DECOMPRESSED, "Number of bytes decompressed.");
        histogramsHelpMap.put(HistogramType.COMPRESSION_TIMES_NANOS, "RocksDB compression time.");
        histogramsHelpMap.put(HistogramType.DECOMPRESSION_TIMES_NANOS, ""); // [rocksdb.decompression.times.nanos]
        histogramsHelpMap.put(HistogramType.READ_NUM_MERGE_OPERANDS, ""); // [rocksdb.read.num.merge_operands]
        histogramsHelpMap.put(HistogramType.BLOB_DB_KEY_SIZE, "Size of keys written to BlobDB.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_VALUE_SIZE, "Size of values written to BlobDB.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_WRITE_MICROS, "BlobDB Put/PutWithTTL/PutUntil/Write latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_GET_MICROS, "BlobDB Get latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_MULTIGET_MICROS, "BlobDB MultiGet latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_SEEK_MICROS, "BlobDB Seek/SeekToFirst/SeekToLast/SeekForPrev latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_NEXT_MICROS, "BlobDB Next latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_PREV_MICROS, "BlobDB Prev latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_BLOB_FILE_WRITE_MICROS, "Blob file write latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_BLOB_FILE_READ_MICROS, "Blob file read latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_BLOB_FILE_SYNC_MICROS, "Blob file sync latency.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_GC_MICROS, "BlobDB garbage collection time.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_COMPRESSION_MICROS, "BlobDB compression time.");
        histogramsHelpMap.put(HistogramType.BLOB_DB_DECOMPRESSION_MICROS, "BlobDB decompression time.");
        histogramsHelpMap.put(HistogramType.FLUSH_TIME, "Time spent flushing memtable to disk.");
        /*
        histogramsHelpMap.put(HistogramType.SST_BATCH_SIZE, "rocksdb.sst.batch.size");
        histogramsHelpMap.put(HistogramType.NUM_INDEX_AND_FILTER_BLOCKS_READ_PER_LEVEL, "rocksdb.num.index.and.filter.blocks.read.per.level");
        histogramsHelpMap.put(HistogramType.NUM_DATA_BLOCKS_READ_PER_LEVEL, "rocksdb.num.data.blocks.read.per.level");
        histogramsHelpMap.put(HistogramType.NUM_SST_READ_PER_LEVEL, "rocksdb.num.sst.read.per.level");
        */
    }

    // a map of "HistogramType" to Prometheus Metric name
    private void initHistograms2NameMap() {
        for (HistogramType histogramType : HistogramType.values()) {
            if(histogramType != HistogramType.HISTOGRAM_ENUM_MAX) {
                final String name = histogramsNameMap.get(histogramType);
                final String metricName = name.replace('.', '_');
                histograms2NameMap.put(histogramType, metricName);
            }
        }
    }

    /**
     * Expose RocksDB statistics info to Prometheus metrics format.
     */
    @Override
    public List<MetricFamilySamples> collect() {
        List<Collector.MetricFamilySamples> mfs = new ArrayList<>();

        // convert 'Ticker' to Prometheus 'Counter'
        for (TickerType tickerType : TickerType.values()) {
            if(tickerType != TickerType.TICKER_ENUM_MAX) {
                final String name = countersNameMap.get(tickerType);
                final String helpInfo = tickersHelpMap.get(tickerType);
                final double read = (double) stats.getTickerCount(tickerType);
                CounterMetricFamily counterMetricFamily = new CounterMetricFamily(name, helpInfo, read);
                mfs.add(counterMetricFamily);
            }
        }

        // convert 'Histogram' to Prometheus 'Summary'
        for (HistogramType histogramType : HistogramType.values()) {
            if (histogramType != HistogramType.HISTOGRAM_ENUM_MAX) {
                final HistogramData histogramData = stats.getHistogramData(histogramType);
                final String name = histograms2NameMap.get(histogramType);
                final String helpInfo = histogramsHelpMap.get(histogramType);

                final double count = (double) histogramData.getCount();
                final double sum = (double) histogramData.getSum();
                final double p95 = histogramData.getPercentile95();
                final double p99 = histogramData.getPercentile99();
                // With labels. Record 95th percentile as p95, and 99th percentile as p99.
                SummaryMetricFamily labeledSummary = new SummaryMetricFamily(name, helpInfo,
                        Collections.emptyList(), Arrays.asList(.95, .99));

                labeledSummary.addMetric(Collections.emptyList(), count, sum, Arrays.asList(p95, p99));

                mfs.add(labeledSummary);
            }
        }
        return mfs;
    }
}

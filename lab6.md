# Lab 6 - RocksDB Statistics

[Chinses version](lab6_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab6/) **

RocksDB `Statistics` provides cumulative stats over time. It serves different function from DB properties and perf and IO Stats context: statistics accumulate stats for history, while DB properties report current state of the database; DB statistics give an aggregated view across all operations, whereas perf and IO stats context allow us to look inside of individual operations.

## Usage

Function `CreateDBStatistics()` creates a statistics object.

Here is an example to pass it to one RocksDB:

```java
final Statistics statistics = new Statistics();
final Options options = new Options();

options.setCreateIfMissing(true);
options.setStatistics(statistics);
```

Technically, you can create a statistics object and pass to multiple DBs. Then the statistics object will contain aggregated values for all those DBs. Note that some stats are undefined and have no meaningful information across multiple DBs. One such statistic is "rocksdb.sequence.number".

## Stats Level And Performance Costs

The overhead of statistics is usually small but non-negligible. We usually observe an overhead of **5%** ~ **10%**.

Stats are implemented using atomic integers (atomic increments). Furthermore, stats measuring time duration require to calls the get the current time. Both of the atomic increment and timing functions introduce overhead, which varies across different platforms.

## Access The Stats

### Stats Types

There are two types of stats, `ticker` and `histogram`.

The `ticker` type is represented by 64-bit unsigned integer. **The value never decreases or resets**. Ticker stats are used to measure `counters` (e.g. "rocksdb.block.cache.hit"), `cumulative bytes` (e.g. "rocksdb.bytes.written") or `time` (e.g. "rocksdb.l0.slowdown.micros").

The `histogram` type measures distribution of a stat across all operations. Most of the histograms are for distribution of duration of a DB operation. Taking "rocksdb.db.get.micros" as an example, we measure time spent on each Get() operation and calculate the distribution for all of them.

### Print Human Readable String

We can get a human readable string of all the counters by calling ToString().

> StatisticsDemo.java

```java
import org.rocksdb.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StatisticsDemo {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws RocksDBException {
        final Statistics statistics = new Statistics();
        final Options options = new Options();

        options.setCreateIfMissing(true);
        options.setStatistics(statistics);

        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        final byte[] key = "some-key".getBytes(UTF_8);
        final byte[] value = "some-value".getBytes(UTF_8);

        db.put(key, value);
        for(int i = 0; i < 10; i++) {
            db.get(key);
        }

        System.out.println(statistics);

        // make sure you disposal necessary RocksDB objects
        db.close();
        options.close();
    }
}
```

Below is the result after demo program finished:

```sh
rocksdb.block.cache.miss COUNT : 0
rocksdb.block.cache.hit COUNT : 0
rocksdb.block.cache.add COUNT : 0
rocksdb.block.cache.add.failures COUNT : 0
rocksdb.block.cache.index.miss COUNT : 0
rocksdb.block.cache.index.hit COUNT : 0
rocksdb.block.cache.index.add COUNT : 0
rocksdb.block.cache.index.bytes.insert COUNT : 0
...
...
rocksdb.db.get.micros P50 : 0.625000 P95 : 28.000000 P99 : 31.000000 P100 : 31.000000 COUNT : 10 SUM : 39
rocksdb.db.write.micros P50 : 218.000000 P95 : 218.000000 P99 : 218.000000 P100 : 218.000000 COUNT : 1 SUM : 218
rocksdb.compaction.times.micros P50 : 0.000000 P95 : 0.000000 P99 : 0.000000 P100 : 0.000000 COUNT : 0 SUM : 0
rocksdb.compaction.times.cpu_micros P50 : 0.000000 P95 : 0.000000 P99 : 0.000000 P100 : 0.000000 COUNT : 0 SUM : 0
...
...
rocksdb.db.flush.micros P50 : 0.000000 P95 : 0.000000 P99 : 0.000000 P100 : 0.000000 COUNT : 0 SUM : 0
rocksdb.sst.batch.size P50 : 0.000000 P95 : 0.000000 P99 : 0.000000 P100 : 0.000000 COUNT : 0 SUM : 0
```

To make each metric easy to understand, below are the explainations retrieve from RocksDB soruce code:

### Ticker metrics

Metric name   | Type  | Description | Sample
--------------|:-----:|-------------|------------------------
rocksdb.blobdb.blob.file.bytes.read|ticker|# of bytes read from blob file.|0
rocksdb.blobdb.blob.file.bytes.written|ticker|# of bytes written to blob file.|0
rocksdb.blobdb.blob.file.synced|ticker|# of times a blob files being synced.|0
rocksdb.blobdb.blob.index.evicted.count|ticker|# of blob index evicted from base DB by BlobDB compaction filter because of corresponding file deleted.|0
rocksdb.blobdb.blob.index.evicted.size|ticker|Size of blob index evicted from base DB by BlobDB compaction filter because of corresponding file deleted.|0
rocksdb.blobdb.blob.index.expired.count|ticker|# of blob index evicted from base DB by BlobDB compaction filter because of expiration.|0
rocksdb.blobdb.blob.index.expired.size|ticker|Size of blob index evicted from base DB by BlobDB compaction filter because of expiration.|0
rocksdb.blobdb.bytes.read|ticker|# of bytes (keys + value) read from BlobDB.|0
rocksdb.blobdb.bytes.written|ticker|# of bytes (key + value) written to BlobDB.|0
rocksdb.blobdb.fifo.bytes.evicted|ticker|# of bytes in the blob files evicted because of BlobDB is full.|0
rocksdb.blobdb.fifo.num.files.evicted|ticker|# of blob files evicted because of BlobDB is full.|0
rocksdb.blobdb.fifo.num.keys.evicted|ticker|# of keys in the blob files evicted because of BlobDB is full.|0
rocksdb.blobdb.gc.bytes.expired|ticker|# of bytes drop by BlobDB garbage collection because of expiration.|0
rocksdb.blobdb.gc.bytes.overwritten|ticker|# of bytes drop by BlobDB garbage collection because they had been overwritten.|0
rocksdb.blobdb.gc.bytes.relocated|ticker|# of bytes relocated to new blob file by garbage collection.|0
rocksdb.blobdb.gc.failures|ticker|# of BlobDB garbage collection failures.|0
rocksdb.blobdb.gc.num.files|ticker|# of blob files being garbage collected.|0
rocksdb.blobdb.gc.num.keys.expired|ticker|# of keys drop by BlobDB garbage collection because of expiration.|0
rocksdb.blobdb.gc.num.keys.overwritten|ticker|# of keys drop by BlobDB garbage collection because they had been overwritten.|0
rocksdb.blobdb.gc.num.keys.relocated|ticker|# of keys relocated to new blob file by garbage collection.|0
rocksdb.blobdb.gc.num.new.files|ticker|# of blob files generated by garbage collection.|0
rocksdb.blobdb.num.get|ticker|# of Get to BlobDB.|0
rocksdb.blobdb.num.keys.read|ticker|# of keys read from BlobDB.|0
rocksdb.blobdb.num.keys.written|ticker|# of keys written to BlobDB.|0
rocksdb.blobdb.num.multiget|ticker|# of MultiGet to BlobDB.|0
rocksdb.blobdb.num.next|ticker|# of Next to BlobDB iterator.|0
rocksdb.blobdb.num.prev|ticker|# of Prev to BlobDB iterator.|0
rocksdb.blobdb.num.put|ticker|# of Put/PutTTL/PutUntil to BlobDB.|0
rocksdb.blobdb.num.seek|ticker|# of Seek/SeekToFirst/SeekToLast/SeekForPrev to BlobDB iterator.|0
rocksdb.blobdb.num.write|ticker|# of Write to BlobDB.|0
rocksdb.blobdb.write.blob|ticker|# of keys written by BlobDB as non-TTL blob value.|0
rocksdb.blobdb.write.blob.ttl|ticker|# of keys written by BlobDB as TTL blob value.|0
rocksdb.blobdb.write.inlined|ticker|# of keys written by BlobDB as non-TTL inlined value.|0
rocksdb.blobdb.write.inlined.ttl|ticker|# of keys written by BlobDB as TTL inlined value.|0
rocksdb.block.cache.add|ticker|# of times block added to block cache|0
rocksdb.block.cache.add.failures|ticker|# of failures when adding blocks to block cache.|0
rocksdb.block.cache.add.redundant|ticker||0
rocksdb.block.cache.bytes.read|ticker|# of bytes read from cache.|0
rocksdb.block.cache.bytes.write|ticker|# of bytes written into cache.|0
rocksdb.block.cache.compression.dict.add|ticker||0
rocksdb.block.cache.compression.dict.add.redundant|ticker||0
rocksdb.block.cache.compression.dict.bytes.evict|ticker||0
rocksdb.block.cache.compression.dict.bytes.insert|ticker||0
rocksdb.block.cache.compression.dict.hit|ticker||0
rocksdb.block.cache.compression.dict.miss|ticker||0
rocksdb.block.cache.data.add|ticker|# of data blocks added to block cache.|0
rocksdb.block.cache.data.add.redundant|ticker||0
rocksdb.block.cache.data.bytes.insert|ticker|# of bytes of data blocks inserted into cache|0
rocksdb.block.cache.data.hit|ticker|# of times cache hit when accessing data block from block cache.|0
rocksdb.block.cache.data.miss|ticker|# of times cache miss when accessing data block from block cache.|0
rocksdb.block.cache.filter.add|ticker|# of filter blocks added to block cache.|0
rocksdb.block.cache.filter.add.redundant|ticker||0
rocksdb.block.cache.filter.bytes.evict|ticker|# of bytes of bloom filter block erased from cache|0
rocksdb.block.cache.filter.bytes.insert|ticker|# of bytes of bloom filter blocks inserted into cache|0
rocksdb.block.cache.filter.hit|ticker|# of times cache hit when accessing filter block from block cache.|0
rocksdb.block.cache.filter.miss|ticker|# of times cache miss when accessing filter block from block cache.|0
rocksdb.block.cache.hit|ticker|# of times total block cache hit|0
rocksdb.block.cache.index.add|ticker|# of index blocks added to block cache.|0
rocksdb.block.cache.index.add.redundant|ticker||0
rocksdb.block.cache.index.bytes.evict|ticker|# of bytes of index block erased from cache|0
rocksdb.block.cache.index.bytes.insert|ticker|# of bytes of index blocks inserted into cache|0
rocksdb.block.cache.index.hit|ticker|# of times cache hit when accessing index block from block cache.|0
rocksdb.block.cache.index.miss|ticker|# of times cache miss when accessing index block from block cache.|0
rocksdb.block.cache.miss|ticker|# of times total block cache misses|0
rocksdb.block.cachecompressed.add|ticker|# of blocks added to compressed block cache.|0
rocksdb.block.cachecompressed.add.failures|ticker|# of times failures occurred when adding blocks to compressed block cache.|0
rocksdb.block.cachecompressed.hit|ticker|# of times hit in the compressed block cache.|0
rocksdb.block.cachecompressed.miss|ticker|# of times miss in the compressed block cache.|0
rocksdb.bloom.filter.full.positive|ticker|# of times bloom FullFilter has not avoided the reads.|0
rocksdb.bloom.filter.full.true.positive|ticker|# of times bloom FullFilter has not avoided the reads and data actually exist.|0
rocksdb.bloom.filter.micros|ticker||0
rocksdb.bloom.filter.prefix.checked|ticker|# of times bloom was checked before creating iterator on a file|0
rocksdb.bloom.filter.prefix.useful|ticker|# of times the bloom check was useful in avoiding iterator creation (and thus likely IOPs).|0
rocksdb.bloom.filter.useful|ticker|# of times bloom filter has avoided file reads.|0
rocksdb.bytes.read|ticker|# of uncompressed bytes read from DB::Get(). |100
rocksdb.bytes.written|ticker|# of uncompressed bytes issued by DB::Put(), DB::Delete(), DB::Merge(), and DB::Write().|33
rocksdb.compact.read.bytes|ticker|Bytes read during compaction.|0
rocksdb.compact.read.marked.bytes|ticker|Number of bytes read for compaction due to marked operation|0
rocksdb.compact.read.periodic.bytes|ticker|Number of bytes read for compaction due to periodic operation|0
rocksdb.compact.read.ttl.bytes|ticker|Number of bytes read for compaction due to ttl operation|0
rocksdb.compact.write.bytes|ticker|Bytes written during compaction.|880
rocksdb.compact.write.marked.bytes|ticker|Number of bytes written for compaction due to marked operation|0
rocksdb.compact.write.periodic.bytes|ticker|Number of bytes written for compaction due to periodic operation|0
rocksdb.compact.write.ttl.bytes|ticker|Number of bytes written for compaction due to ttl operation|0
rocksdb.compaction.cancelled|ticker|If a compaction was cancelled in sfm to prevent ENOSPC|0
rocksdb.compaction.key.drop.new|ticker|key was written with a newer value.|0
rocksdb.compaction.key.drop.obsolete|ticker|Also includes keys dropped for range del. The key is obsolete.|0
rocksdb.compaction.key.drop.range_del|ticker|key was covered by a range tombstone.|0
rocksdb.compaction.key.drop.user|ticker|User compaction function has dropped the key.|0
rocksdb.compaction.optimized.del.drop.obsolete|ticker|Deletions obsoleted before bottom level due to file gap optimization.|0
rocksdb.compaction.range_del.drop.obsolete|ticker|all keys in range were deleted.|0
rocksdb.db.iter.bytes.read|ticker|The number of uncompressed bytes read from an iterator. Includes size of key and value.|0
rocksdb.db.mutex.wait.micros|ticker|The wait time for db mutex. Disabled by default. To enable it set stats level to StatsLevel.ALL|0
rocksdb.files.deleted.immediately|ticker|# of files deleted immediately by delete scheduler|0
rocksdb.files.marked.trash|ticker|# of files marked as trash by delete scheduler|0
rocksdb.filter.operation.time.nanos|ticker|# of duration (in nano sec) spent for filter operation.|0
rocksdb.flush.write.bytes|ticker|# of bytes written during flush.|0
rocksdb.getupdatessince.calls|ticker|# of calls to RocksDB.getUpdatesSince(long). |0
rocksdb.l0.hit|ticker|# of Get() queries served by L0|0
rocksdb.l0.num.files.stall.micros|ticker|write throttle because of too many files in L0.|0
rocksdb.l0.slowdown.micros|ticker|Time system had to wait to do LO-L1 compactions.|0
rocksdb.l1.hit|ticker|# of Get() queries served by L1|0
rocksdb.l2andup.hit|ticker|# of Get() queries served by L2 and up|0
rocksdb.memtable.compaction.micros|ticker|Time system had to wait to move memtable to L1.|0
rocksdb.memtable.hit|ticker|# of memtable hits.|10
rocksdb.memtable.miss|ticker|# of memtable misses.|0
rocksdb.merge.operation.time.nanos|ticker||0
rocksdb.no.file.closes|ticker||0
rocksdb.no.file.errors|ticker||0
rocksdb.no.file.opens|ticker||4
rocksdb.num.iterator.created|ticker|Number of iterators created.|0
rocksdb.num.iterator.deleted|ticker|Number of iterators deleted.|0
rocksdb.num.iterators|ticker|Number of iterators created.|0
rocksdb.number.block.compressed|ticker|# of compressions executed|0
rocksdb.number.block.decompressed|ticker|# of decompressions executed|0
rocksdb.number.block.not_compressed|ticker||2
rocksdb.number.db.next|ticker|The number of calls to next.|0
rocksdb.number.db.next.found|ticker|The number of calls to next that returned data.|0
rocksdb.number.db.prev|ticker|he number of calls to prev.|0
rocksdb.number.db.prev.found|ticker|The number of calls to prev that returned data.|0
rocksdb.number.db.seek|ticker|The number of calls to seek.|0
rocksdb.number.db.seek.found|ticker|The number of calls to seek that returned data.|0
rocksdb.number.deletes.filtered|ticker|Number of deletes records that were not required to be written to storage because key does not exist.|0
rocksdb.number.direct.load.table.properties|ticker|Number of table's properties loaded directly from file, without creating table reader object.|0
rocksdb.number.iter.skip|ticker|Number of internal skipped during iteration|0
rocksdb.number.keys.read|ticker|Number of Keys read.|10
rocksdb.number.keys.updated|ticker|Number keys updated, if inplace update is enabled|0
rocksdb.number.keys.written|ticker|Number of keys written to the database via the Put and Write call's.|1
rocksdb.number.merge.failures|ticker|Number of merge records that were not required to be written to storage because key does not exist.|0
rocksdb.number.multiget.bytes.read|ticker|Number of MultiGet bytes read.|0
rocksdb.number.multiget.get|ticker|Number of MultiGet calls.|0
rocksdb.number.multiget.keys.found|ticker|Number of MultiGet keys found (vs number requested)|0
rocksdb.number.multiget.keys.read|ticker|Number of MultiGet keys read.|0
rocksdb.number.rate_limiter.drains|ticker|Number of refill intervals where rate limiter's bytes are fully consumed.|0
rocksdb.number.reseeks.iteration|ticker|Number of times we had to reseek inside an iteration to skip over large number of keys with same userkey.|0
rocksdb.number.superversion_acquires|ticker||1
rocksdb.number.superversion_cleanups|ticker||0
rocksdb.number.superversion_releases|ticker||0
rocksdb.persistent.cache.hit|ticker|# persistent cache hit|0
rocksdb.persistent.cache.miss|ticker|# persistent cache miss|0
rocksdb.rate.limit.delay.millis|ticker||0
rocksdb.read.amp.estimate.useful.bytes|ticker|Estimate of total bytes actually used.|0
rocksdb.read.amp.total.read.bytes|ticker|Total size of loaded data blocks.|0
rocksdb.row.cache.hit|ticker|# of row cache hit|0
rocksdb.row.cache.miss|ticker|# of row cache miss|0
rocksdb.sim.block.cache.hit|ticker|# total simulation block cache hits|0
rocksdb.sim.block.cache.miss|ticker|# total simulation block cache misses|0
rocksdb.stall.micros|ticker|Writer has to wait for compaction or flush to finish.|0
rocksdb.txn.get.tryagain|ticker|# of times ::Get returned TryAgain due to expired snapshot seq|0
rocksdb.txn.overhead.duplicate.key|ticker|# of times we checked a batch for duplicate keys.|0
rocksdb.txn.overhead.mutex.old.commit.map|ticker|# of times old_commit_map_mutex_ is acquired in the fast path.|0
rocksdb.txn.overhead.mutex.prepare|ticker|These counters indicate a performance issue in WritePrepared transactions. We should not seem them ticking them much. # of times prepare_mutex_ is acquired in the fast path.|0
rocksdb.txn.overhead.mutex.snapshot|ticker|# of times snapshot_mutex_ is acquired in the fast path.|0
rocksdb.wal.bytes|ticker|Number of bytes written to WAL.|33
rocksdb.wal.synced|ticker|Number of times WAL sync is done.|0
rocksdb.write.other|ticker|Equivalent to writes done for others.|0
rocksdb.write.self|ticker|Writes can be processed by requesting thread or by the thread at the head of the writers queue.|1
rocksdb.write.timeout|ticker|Number of writes ending up with timed-out.|0
rocksdb.write.wal|ticker|Number of Write calls that request WAL.|2

### Histogram metrics

Metric name   | Type  | Description | Sample
--------------|:-----:|-------------|------------------------
rocksdb.blobdb.blob.file.read.micros|histogram|Blob file read latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.blob.file.sync.micros|histogram|Blob file sync latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.blob.file.write.micros|histogram|Blob file write latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.compression.micros|histogram|BlobDB compression time.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.decompression.micros|histogram|BlobDB decompression time.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.gc.micros|histogram|BlobDB garbage collection time.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.get.micros|histogram|BlobDB Get lagency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.key.size|histogram|Size of keys written to BlobDB.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.multiget.micros|histogram|BlobDB MultiGet latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.next.micros|histogram|BlobDB Next latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.prev.micros|histogram|BlobDB Prev latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.seek.micros|histogram|BlobDB Seek/SeekToFirst/SeekToLast/SeekForPrev latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.value.size|histogram|Size of values written to BlobDB.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.blobdb.write.micros|histogram|BlobDB Put/PutWithTTL/PutUntil/Write latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.bytes.compressed|histogram|number of bytes compressed.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.bytes.decompressed|histogram|number of bytes decompressed. number of bytes is when uncompressed; i.e. before/after respectively|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.bytes.per.multiget|histogram|Value size distribution of multiget operation.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.bytes.per.read|histogram|Value size distribution of read operation.|P50:10, P95:10, P99:10, P100:10, COUNT:10, SUM:100
rocksdb.bytes.per.write|histogram|Value size distribution of write operation.|P50:33, P95:33, P99:33, P100:33, COUNT:1, SUM:33
rocksdb.compaction.outfile.sync.micros|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.compaction.times.cpu_micros|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.compaction.times.micros|histogram|RocksDB compaction time.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.compression.times.nanos|histogram|RocksDB compression time.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.db.flush.micros|histogram|Time spent flushing memtable to disk.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.db.get.micros|histogram|RocksDB Get lagency.|P50:0.555556, P95:18.5, P99:19, P100:19, COUNT:10, SUM:23
rocksdb.db.multiget.micros|histogram|RocksDB MultiGet latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.db.seek.micros|histogram|RocksDB Seek/SeekToFirst/SeekToLast/SeekForPrev latency.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.db.write.micros|histogram||P50:172, P95:172, P99:172, P100:172, COUNT:1, SUM:172
rocksdb.db.write.stall|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.decompression.times.nanos|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.hard.rate.limit.delay.count|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.l0.slowdown.count|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.manifest.file.sync.micros|histogram||P50:3334, P95:3334, P99:3334, P100:3334, COUNT:1, SUM:3334
rocksdb.memtable.compaction.count|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.num.files.stall.count|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.num.subcompactions.scheduled|histogram|The number of subcompactions actually scheduled during a compaction.|P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.numfiles.in.singlecompaction|histogram||P50:4, P95:4, P99:4, P100:4, COUNT:1, SUM:4
rocksdb.read.block.compaction.micros|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.read.block.get.micros|histogram||P50:7, P95:7, P99:7, P100:7, COUNT:4, SUM:23
rocksdb.read.num.merge_operands|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.soft.rate.limit.delay.count|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.sst.batch.size|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.sst.read.micros|histogram||P50:0.8, P95:6.8, P99:7, P100:7, COUNT:16, SUM:35
rocksdb.subcompaction.setup.times.micros|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.table.open.io.micros|histogram|Time spent in IO during table open.|P50:130, P95:152, P99:152, P100:152, COUNT:4, SUM:465
rocksdb.table.sync.micros|histogram||P50:3506, P95:3506, P99:3506, P100:3506, COUNT:1, SUM:3506
rocksdb.wal.file.sync.micros|histogram||P50:0, P95:0, P99:0, P100:0, COUNT:0, SUM:0
rocksdb.write.raw.block.micros|histogram||P50:0.666667, P95:1.8, P99:1.96, P100:2, COUNT:4, SUM:4

## RocksDB Tuning

As mentioned in [lab5](lab5.md) , RocksDB is very flexible and you can tune it for variety of workloads and storage technologies. Whenever tuning RocksDB , remember to evaluate these metrics and know how RocksDB behave after some knobs are turning. For more detials, see [RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide).

Back to main menu >>  [README](README.md)

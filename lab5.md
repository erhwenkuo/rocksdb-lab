# Lab 5 - RocksDB Benchmark

[Chinses version](lab5_zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab5/) **


## JMH Benchmarks for RocksJava

These are micro-benchmarks for RocksJava functionality, using [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/).

If you are not familiar with JMH, then I would recommend reading [Jenkov JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html).

Below benchmarks are running on my notebook:

* Brand: Lenovo ThinkPad T15
* OS : Ubuntu 20.04.1 LTS
* CPU: Intel(R) Core(TM) i7-8650U CPU @ 1.90GHz, 8 cores
* Mem: 16 GB
* HDD: 512GB SSD, M.2 2280, PCIe Gen3x4, OPAL2.0, TLC

### 1. PutBenchmarks

The benchmarks measure the throughput of RocksDB `put()` operation with different number of column families setup:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

The benchmarks program will try to write k/v to different column families round-robinly.

Below is the result:

```sh
$ java -Xmx4G -jar target/benchmarks.jar io.github.erhwenkuo.rocksdb.jmh.PutBenchmarks

# JMH version: 1.27
# VM version: JDK 1.8.0_275, OpenJDK 64-Bit Server VM, 25.275-b01
# VM invoker: /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
# VM options: -Xmx4G
# JMH blackhole mode: full blackhole + dont-inline hint
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time

..
..

Benchmark                      (columnFamilyTestType)        Score       Error  Units
PutBenchmarks.put                    no_column_family   313982.849 ±  9477.245  ops/s
PutBenchmarks.put                     1_column_family   302635.930 ± 13098.287  ops/s
PutBenchmarks.put                  20_column_families   313830.605 ± 13397.900  ops/s
PutBenchmarks.put                 100_column_families   314369.121 ±  5761.873  ops/s
```

Observations:

* There is not performance gap if we use multiple column families for write throughtput

### 2. GetBenchmarks

The benchmarks measure the throughput of RocksDB `get()` operation with different number of column families setup:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

Before bechmarking, there are `100,000` k/v pairs loading into each column family. The benchmarks program will try to get k/v from different column families round-robinly.

Below is the result:

```sh
$ java -Xmx4G -jar target/benchmarks.jar io.github.erhwenkuo.rocksdb.jmh.GetBenchmarks

# JMH version: 1.27
# VM version: JDK 1.8.0_275, OpenJDK 64-Bit Server VM, 25.275-b01
# VM invoker: /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
# VM options: -Xmx4G
# JMH blackhole mode: full blackhole + dont-inline hint
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time

..
..

Benchmark                      (columnFamilyTestType)        Score       Error  Units
GetBenchmarks.get                    no_column_family  3028312.057 ± 32455.312  ops/s
GetBenchmarks.get                     1_column_family  3020951.011 ± 26374.264  ops/s
GetBenchmarks.get                  20_column_families   739486.681 ±  8045.435  ops/s
GetBenchmarks.get                 100_column_families   529727.129 ±  4478.364  ops/s
```

Observations:

* The read throughtput is getting worse when the size column familiy increased. (Mm...interesting)

### 3. MultiGetBenchmarks

Batching and caching are most famous ways to optimise client-server service throughput. The major reason is client need to utilize network to communicate with server and get back the result. Of course this is one of the major initiatives that RocksDB start with.

The benchmarks measure the throughput of RocksDB `multiGetAsList()` operation with different number of column families setup:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

Also the size of keys to `batch get` will be evaluatd from 10, 100, 1000 & 10,000.

Below is the result:

```sh
$ java -Xmx4G -jar target/benchmarks.jar io.github.erhwenkuo.rocksdb.jmh.MultiGetBenchmarks
# JMH version: 1.27
# VM version: JDK 1.8.0_275, OpenJDK 64-Bit Server VM, 25.275-b01
# VM invoker: /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
# VM options: -Xmx4G
# JMH blackhole mode: full blackhole + dont-inline hint
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time

..
..

Benchmark                      (columnFamilyTestType)  (keyCount)  (multiGetSize)   Mode  Cnt        Score       Error  Units
MultiGetBenchmarks.multiGet10        no_column_family      100000              10  thrpt   25   253591.587 ±  2157.069  ops/s
MultiGetBenchmarks.multiGet10        no_column_family      100000             100  thrpt   25    31167.328 ±   133.967  ops/s
MultiGetBenchmarks.multiGet10        no_column_family      100000            1000  thrpt   25     3166.074 ±    20.629  ops/s
MultiGetBenchmarks.multiGet10        no_column_family      100000           10000  thrpt   25      305.738 ±     1.095  ops/s
MultiGetBenchmarks.multiGet10         1_column_family      100000              10  thrpt   25    61471.137 ±   446.092  ops/s
MultiGetBenchmarks.multiGet10         1_column_family      100000             100  thrpt   25     6536.296 ±    48.163  ops/s
MultiGetBenchmarks.multiGet10         1_column_family      100000            1000  thrpt   25      648.957 ±     4.954  ops/s
MultiGetBenchmarks.multiGet10         1_column_family      100000           10000  thrpt   25       65.524 ±     0.555  ops/s
MultiGetBenchmarks.multiGet10      20_column_families      100000              10  thrpt   25    60413.099 ±   524.949  ops/s
MultiGetBenchmarks.multiGet10      20_column_families      100000             100  thrpt   25     6453.248 ±    53.687  ops/s
MultiGetBenchmarks.multiGet10      20_column_families      100000            1000  thrpt   25      648.484 ±     2.801  ops/s
MultiGetBenchmarks.multiGet10      20_column_families      100000           10000  thrpt   25       64.448 ±     1.828  ops/s
MultiGetBenchmarks.multiGet10     100_column_families      100000              10  thrpt   25    60802.072 ±   616.661  ops/s
MultiGetBenchmarks.multiGet10     100_column_families      100000             100  thrpt   25     6486.085 ±    41.553  ops/s
MultiGetBenchmarks.multiGet10     100_column_families      100000            1000  thrpt   25      641.193 ±     4.510  ops/s
MultiGetBenchmarks.multiGet10     100_column_families      100000           10000  thrpt   25       66.426 ±     0.627  ops/s
```

Observations:

* The ops/s vs multiGetSize are changed linearly, which indicates no performance gain if we utilize `batch get` operation
* The read throughtput is getting worse when configured with extra column familiy other than 'default'. (Mm...interesting)

### 4. ComparatorBenchmarks

RocksDB support key range scan & iteration, and by default the keys would be sort by *bytes lexicographically*. There are two build-in key comparators which control the sorting behavior:

* BYTEWISE_COMPARATOR - Sorts all keys in `ascending` bytewise
* REVERSE_BYTEWISE_COMPARATOR - Sorts all keys in `descending` bytewise

```java
/**
 * Builtin RocksDB comparators
 *
 * <ol>
 *   <li>BYTEWISE_COMPARATOR - Sorts all keys in ascending bytewise
 *   order.</li>
 *   <li>REVERSE_BYTEWISE_COMPARATOR - Sorts all keys in descending bytewise
 *   order</li>
 * </ol>
 */
public enum BuiltinComparator {
  BYTEWISE_COMPARATOR, REVERSE_BYTEWISE_COMPARATOR
}
```

RocksDB also supports custom comparator when opening a database. The benchmarks compare RocksDB built-in comparators and custom comparators implemented by Java with differnt configurations:

* __native_bytewise__
* __native_reverse_bytewise__
* java_bytewise_non-direct_reused-64_adaptive-mutex
* java_bytewise_non-direct_reused-64_non-adaptive-mutex
* java_bytewise_non-direct_reused-64_thread-local
* java_bytewise_direct_reused-64_adaptive-mutex
* java_bytewise_direct_reused-64_non-adaptive-mutex
* java_bytewise_direct_reused-64_thread-local
* java_bytewise_non-direct_no-reuse
* java_bytewise_direct_no-reuse
* java_reverse_bytewise_non-direct_reused-64_adaptive-mutex
* java_reverse_bytewise_non-direct_reused-64_non-adaptive-mutex
* java_reverse_bytewise_non-direct_reused-64_thread-local
* java_reverse_bytewise_direct_reused-64_adaptive-mutex
* java_reverse_bytewise_direct_reused-64_non-adaptive-mutex
* java_reverse_bytewise_direct_reused-64_thread-local
* java_reverse_bytewise_non-direct_no-reuse
* java_reverse_bytewise_direct_no-reuse

Below is the result:

```sh
$ java -Xmx4G -jar target/benchmarks.jar io.github.erhwenkuo.rocksdb.jmh.ComparatorBenchmarks

# JMH version: 1.27
# VM version: JDK 1.8.0_275, OpenJDK 64-Bit Server VM, 25.275-b01
# VM invoker: /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
# VM options: -Xmx4G
# JMH blackhole mode: full blackhole + dont-inline hint
# Warmup: 5 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time

...
...

Benchmark                                                              (comparatorName)        Score       Error  Units
ComparatorBenchmarks.put                                                native_bytewise   314257.248 ±  9740.467  ops/s
ComparatorBenchmarks.put                                        native_reverse_bytewise   286557.528 ± 13757.533  ops/s
ComparatorBenchmarks.put              java_bytewise_non-direct_reused-64_adaptive-mutex    51320.333 ± 24907.841  ops/s
ComparatorBenchmarks.put          java_bytewise_non-direct_reused-64_non-adaptive-mutex    48291.866 ± 22539.080  ops/s
ComparatorBenchmarks.put                java_bytewise_non-direct_reused-64_thread-local    52862.959 ± 24268.613  ops/s
ComparatorBenchmarks.put                  java_bytewise_direct_reused-64_adaptive-mutex    56104.157 ± 31928.032  ops/s
ComparatorBenchmarks.put              java_bytewise_direct_reused-64_non-adaptive-mutex    55587.771 ± 28741.818  ops/s
ComparatorBenchmarks.put                    java_bytewise_direct_reused-64_thread-local    52293.297 ± 31664.614  ops/s
ComparatorBenchmarks.put                              java_bytewise_non-direct_no-reuse    40775.289 ± 21547.510  ops/s
ComparatorBenchmarks.put                                  java_bytewise_direct_no-reuse    54121.256 ± 26269.448  ops/s
ComparatorBenchmarks.put      java_reverse_bytewise_non-direct_reused-64_adaptive-mutex    34405.800 ±  9427.065  ops/s
ComparatorBenchmarks.put  java_reverse_bytewise_non-direct_reused-64_non-adaptive-mutex    28821.468 ±  8329.140  ops/s
ComparatorBenchmarks.put        java_reverse_bytewise_non-direct_reused-64_thread-local    35671.314 ±  9319.243  ops/s
ComparatorBenchmarks.put          java_reverse_bytewise_direct_reused-64_adaptive-mutex    39887.066 ± 22325.503  ops/s
ComparatorBenchmarks.put      java_reverse_bytewise_direct_reused-64_non-adaptive-mutex    46421.423 ± 14438.643  ops/s
ComparatorBenchmarks.put            java_reverse_bytewise_direct_reused-64_thread-local    44767.306 ± 20439.473  ops/s
ComparatorBenchmarks.put                      java_reverse_bytewise_non-direct_no-reuse    29405.301 ±  3892.824  ops/s
ComparatorBenchmarks.put                          java_reverse_bytewise_direct_no-reuse    33904.750 ± 12166.978  ops/s
```

Observations:

* There is not big different performance gap if we use built-in `native_bytewise` (asending) & `native_reverse_bytewise` (descending)
* The performance of Comparators implemented in Java is always less than their C++ counterparts due to the bridging overhead
* Using Java to implement `comparator` for key sorting would hurt the write performance 6 ~ 10 times compare to built-in `comparator`.


## RocksDB Tuning

RocksDB is very flexible, which is both good and bad. You can tune it for variety of workloads and storage technologies. However, flexibility is not always user-friendly. RocksDB introduced a huge number of tuning options. For more detials, see [RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide).

This bechmark could serve as your base line before you start to tune your RocksDB.

Back to main menu >>  [README](README.md)

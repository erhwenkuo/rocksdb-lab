# Lab 5 - RocksDB Benchmark

[English version](lab5.md)

<img src="docs/rocksdb.png" width="300px"></img>

** [Example Source Code](lab5/) **


## JMH Benchmarks for RocksJava

我們將使用 [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/)來對RocksDB來進行一些效能測試。

如果你不熟悉JMH，那麼我推薦看[Jenkov JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html)。

以下的基準測試都是運行在我的筆記本電腦上:

* 廠牌: Lenovo ThinkPad T15
* 作業系統: Ubuntu 20.04.1 LTS
* CPU: Intel(R) Core(TM) i7-8650U CPU @ 1.90GHz, 8 cores
* 記憶體: 16 GB
* 硬碟: 512GB SSD, M.2 2280, PCIe Gen3x4, OPAL2.0, TLC

### 1. PutBenchmarks

這個RocksDB基準測試使用不同數量的column family設置來衡量`put()`操作的吞吐量:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

這個基準程序將嘗試將`鍵值對`循環寫入不同的column family。

測試結果如下：

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

結果觀察:

* 如果在RocksDB使用多個column family來進行寫入操作似乎並不會影響吞吐量

### 2. GetBenchmarks

這個RocksDB基準測試使用不同數量的column family設置來衡量`get()`操作的吞吐量:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

在進行測試之前會在每個column family預載入`100,000` 組`鍵值對`, 而這個基準程序將嘗試將`鍵值對`循環地從不同的column family中讀出來。

測試結果如下：

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

結果觀察:

* 當column family的數量增加時，讀取吞吐量將變得越來越差。

### 3. MultiGetBenchmarks

`批資處理`和`緩存`是優化client-server服務吞吐量的最顯著的方法。主要原因是client需要經由網路與服務器通信並獲取結果, 這個網路呼叫往返的延遲是相當高的。當然，這也是驅動開發RocksDB的主要原因之一。

這個RocksDB基準測試使用不同數量的column family設置來衡量`multiGetAsList()`操作的吞吐量:

* no_column_family
* 1_column_family
* 20_column_families
* 100_column_families

`batch get`將比對四種不同批次數量的操作（10、100、1000和10,000）來進行評估。

測試結果如下：

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

結果觀察:

* `ops`與`multiGetSize`呈相對應的線性變化，這表示使用`batch get`操作並不會提高性能
* 使用"default"以外的其他額外column family配置時，讀取吞吐量會變差

### 4. ComparatorBenchmarks

RocksDB支持鍵範圍掃描和迭代，預設的情況下，鍵將按 *bytes lexicographically*　排序。在ＲocksDB有兩個內建的key comparators來控制排序行為:

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

RocksDB還支持自定義的comparator。基準測試將對RocksDB內建的comparator和Java實現的具有不同配置的自定義comparator進行了比較:

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

測試結果如下：

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

結果觀察:

* 如果我們使用內建的 `native_bytewise` (asending) 與 `native_reverse_bytewise` (descending)，則性能差異很小
* 由於橋接的開銷，用Java實現的comparator的性能始終低於C++實現的comparator性能
* 與內建的comparator相比，使用Java實現comparator進行鍵排序會損害6~10倍的寫入性能

## RocksDB Tuning

RocksDB非常的有彈性, 你可以針對各種不同性質的工作負載和存儲技術進行調整。但是這樣的靈活性(需要去調整不同的設定值)並不總是對用戶友好的與理解。 RocksDB有著大量可調整選項。想了解更多細節，請參見[RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide)。

在開始調整RocksDB之前，這些基準測試可以用作調整驗證的基準。

返回主目錄 >>  [README](README_zh-tw.md)

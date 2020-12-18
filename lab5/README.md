# JMH Benchmarks for RocksJava

These are micro-benchmarks for RocksJava functionality, using [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/).

The benchmark includes:

* ComparatorBenchmarks
* GetBenchmarks
* MultiGetBenchmarks
* PutBenchmarks


## Usage
This benchmark uses POSIX calls to accurately determine consumed disk space and
only depends on Linux-specific native library wrappers where a range of such
wrappers exists. Operation on non-Linux operating systems is unsupported.

1. Clone this repository and `mvn clean package`
2. Run the benchmark with `java -jar target/benchmarks.jar`

The benchmark offers many parameters, but to reduce execution time they default
to a fast, mechanically-sympathetic workload (ie integer keys, sequential IO)
that should fit in RAM. A default execution takes around 15 minutes on
server-grade hardware (ie 2 x Intel Xeon E5-2667 v3 CPUs, 512 GB RAM etc).

You can append ``-h`` to the ``java -jar`` line for JMH help. For example, use:

  * ``-foe true`` to stop on any error (recommended)
  * ``-rf csv`` to emit a CSV results file (recommended)
  * ``-f 3`` to run three forks for smaller error ranges (recommended)
  * ``-lp`` to list all available parameters
  * ``-p intKey=true,false`` to test both integer and string-based keys
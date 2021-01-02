package io.github.erhwenkuo.rocksdb;

import io.github.erhwenkuo.rocksdb.collector.StatisticInfoCollector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Statistics;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RocksDBMetricsServer {
    static {
        RocksDB.loadLibrary();
    }

    public static void main(String[] args) throws IOException, RocksDBException {
        int port = 9099;
        InetSocketAddress socket = new InetSocketAddress(port);

        // RocksDB
        final Statistics statistics = new Statistics();
        final Options options = new Options();

        options.setCreateIfMissing(true);
        options.setStatistics(statistics);

        final RocksDB db = RocksDB.open(options, "/tmp/testdb");

        // Do some Put/Get operations
        final byte[] key = "some-key".getBytes(UTF_8);
        final byte[] value = "some-value".getBytes(UTF_8);

        db.put(key, value);
        for(int i = 0; i < 10; i++) {
            db.get(key);
        }

        // Customize Prometheus Exporter to expose RocksDB statistics metrics
        new StatisticInfoCollector(statistics).register();

        // A simple web server to expose RocksDB statistics metrics
        new HTTPServer(socket, CollectorRegistry.defaultRegistry);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application Terminating ...");
            System.out.println("Shutdown Hook is running !");
            // make sure you disposal necessary RocksDB objects
            db.close();
            options.close();
            statistics.close();
        }));
    }
}

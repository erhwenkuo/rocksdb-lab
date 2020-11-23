package io.github.erhwenkuo.rocksdbquickstart.repository;

import java.util.Optional;

public interface KVRepository<K, V> {
    boolean save(K key, V value);
    Optional<V> find(K key);
    boolean delete(K key);
}

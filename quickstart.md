# Using RocksDB with Spring Boot and Java

[Chinses version](quickstart-zh-tw.md)

<img src="docs/rocksdb.png" width="300px"></img>

[RocksDB](https://rocksdb.org/) is an embedded key-value store by Facebook, which is a fork of [LevelDB](https://github.com/google/leveldb) by Google. It is used as a storage layer for a number of databases e.g. [CockroachDB](https://www.cockroachlabs.com/). You can use it as an embedded store, a cache (instead of Redis), as a storage layer for your own custom database, file system or storage solution etc.

## Scaffold Springboot Project

You can utilize [spring initializr](https://start.spring.io/) to scaffold a new Springboot project.

![](docs/quickstart/spring-initializr.png)

Click "GENERATE" button and download `rocksdb-quickstart.zip`. 

The project structure looks like:

```sh
rocksdb-quickstart
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── io
    │   │       └── github
    │   │           └── erhwenkuo
    │   │               └── rocksdbquickstart
    │   │                   └── RocksdbQuickstartApplication.java
    │   └── resources
    │       ├── application.properties
    │       ├── static
    │       └── templates
    └── test
        └── java
            └── io
                └── github
                    └── erhwenkuo
                        └── rocksdbquickstart
                            └── RocksdbQuickstartApplicationTests.java
```

## Setup Maven dependecy

Edit `pom.xml`, add the following dependency to bring in RocksDB:

```xml
<!-- https://mvnrepository.com/artifact/org.rocksdb/rocksdbjni -->
<dependency>
  <groupId>org.rocksdb</groupId>
  <artifactId>rocksdbjni</artifactId>
  <version>6.13.3</version>
</dependency>
```

For reference, you can look at my own [pom.xml](rocksdb-quickstart/pom.xml).

## Construct KVRepository for simple key/value service

we want to describe a repository interface through which our app can interact with storage services in general, and RocksDB in particular.

Create a interface `KVRepository.java` under package `repository`:

```java
import java.util.Optional;

public interface KVRepository<K, V> {
    boolean save(K key, V value);
    Optional<V> find(K key);
    boolean delete(K key);
}
```

![](docs/quickstart/kvrepository.png)

Saving, finding and deleting are the basics we require from any key-value store, so we define that. With this interface, we can use any key-value store without changing other parts of our app which is good design. 

## Initialize RocksDB in SpringBoot

Next, we create our RocksDB repository as an implementation of this interface. We will initialize our RocksDB database when the app starts in `initialize()`. RocksDB is a low-level store, so we need to serialize our key-value pairs to bytes before interacting with it in `save()`, `find()` and `delete()`.

Create a class `RocksDBRepository.java`:

```java
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Repository;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Repository
public class RocksDBRepository implements KVRepository<String, Object>{
    private final static String FILE_NAME = "spring-boot-db";
    File baseDir;
    RocksDB db;

    @PostConstruct // execute after the application starts
    void initialize() {
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        baseDir = new File("/tmp/rocks", FILE_NAME);

        try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, baseDir.getAbsolutePath());

            log.info("RocksDB initialized");
        } catch(IOException | RocksDBException e) {
            log.error("Error initializng RocksDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
        }
    }

    @Override
    public boolean save(String key, Object value) {
        log.info("saving value '{}' with key '{}'", value, key);

        try {
            db.put(key.getBytes(), SerializationUtils.serialize(value));
        } catch (RocksDBException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
        }

        return false;
    }

    @Override
    public Optional<Object> find(String key) {
        Object value = null;

        try {
            byte[] bytes = db.get(key.getBytes());
            if (bytes != null)
                value = SerializationUtils.deserialize(bytes);
        } catch (RocksDBException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage()
            );
        }

        log.info("finding key '{}' returns '{}'", key, value);

        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public boolean delete(String key) {
        log.info("deleting key '{}'", key);

        try {
            db.delete(key.getBytes());
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());

            return false;
        }

        return true;
    }
}
```

![](docs/quickstart/rocksdbrepository.png)

## Build a key/value REST API

At this stage, our repository is done but not very usable. Let's add a controller `KVController.java` to enable us interact with the repository:

```java

import io.github.erhwenkuo.rocksdbquickstart.repository.KVRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class KVController {
    private final KVRepository<String, Object> repository;

    public KVController(KVRepository<String, Object> repository) {
        this.repository = repository;
    }

    // curl -iv -X POST -H "Content-Type: application/json" -d '{"bar":"baz"}' http://localhost:8080/api/foo
    @PostMapping(value = "/{key}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> save(@PathVariable("key") String key, @RequestBody Object value) {
        return repository.save(key, value)
                ? ResponseEntity.ok(value)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // curl -iv -X GET -H "Content-Type: application/json" http://localhost:8080/api/foo
    @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> find(@PathVariable("key") String key) {
        return ResponseEntity.of(repository.find(key));
    }

    // curl -iv -X DELETE -H "Content-Type: application/json" http://localhost:8080/api/foo
    @DeleteMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> delete(@PathVariable("key") String key) {
        return repository.delete(key)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

![](docs/quickstart/rocksdbrepository.png)

## Invoke & Test

We declare a simple REST controller that exposes save, find and delete endpoints. That's all there is to it.

Go ahead to start the application.

```sh
$ mvnw spring-boot:run
```

The application will start to listent port:8080:

![](docs/quickstart/start-application.png)

* Create a key/value entry

```sh
curl -X POST -H "Content-Type: application/json" -d '{"bar":"baz"}' http://localhost:8080/api/foo
```

* Get the value back

```sh
curl -X GET -H "Content-Type: application/json" http://localhost:8080/api/foo
```

* Delete the key

```sh
curl -X DELETE -H "Content-Type: application/json" http://localhost:8080/api/foo
```

Back to main menu >>  [README](README.md)

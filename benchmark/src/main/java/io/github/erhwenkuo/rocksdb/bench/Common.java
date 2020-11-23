package io.github.erhwenkuo.rocksdb.bench;

/*-
 * #%L
 * LmdbJava Benchmarks
 * %%
 * Copyright (C) 2016 - 2020 The LmdbJava Open Source Project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static java.lang.Integer.BYTES;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static jnr.posix.POSIXFactory.getPOSIX;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import org.agrona.collections.IntHashSet;
import org.apache.commons.math3.random.BitsStreamGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;

/**
 * Common JMH {@link State} superclass for all DB benchmark states.
 *
 * <p>
 * Members do not reflect the typical code standards of the LmdbJava project due
 * to compliance requirements with JMH {@link Param} and {@link State}.
 */
@State(Benchmark)
public class Common {

    static final byte[] RND_MB = new byte[1_048_576];
    static final int STRING_KEY_LENGTH = 16;
    private static final POSIX POSIX = getPOSIX();
    private static final BitsStreamGenerator RND = new MersenneTwister();
    private static final int S_BLKSIZE = 512; // from sys/stat.h
    private static final File TMP_BENCH;

    File compact;

    CRC32 crc;

    /**
     * Keys are always an integer, however they are actually stored as integers
     * (taking 4 bytes) or as zero-padded 16 byte strings. Storing keys as
     * integers offers a major performance gain.
     */
    @Param("true")
    boolean intKey;

    /**
     * Determined during {@link #setup()} based on {@link #intKey} value.
     */
    int keySize;
    /**
     * Keys in designated (random/sequential) order.
     */
    int[] keys;

    /**
     * Number of entries to read/write to the database.
     */
    @Param("1000000")
    int num;

    /**
     * Whether the keys are to be inserted into the database in sequential order
     * (and in the "readKeys" case, read back in that order). For LMDB, sequential
     * inserts use {@link org.lmdbjava.PutFlags#MDB_APPEND} and offer a major
     * performance gain. If this field is false, the append flag will not be used
     * and the keys will instead be inserted (and read back via "readKeys") in a
     * random order.
     */
    @Param("true")
    boolean sequential;

    File tmp;

    /**
     * Whether the values contain random bytes or are simply the same as the key.
     * If true, the random bytes are obtained sequentially from a 1 MB random byte
     * buffer.
     */
    @Param("false")
    boolean valRandom;

    /**
     * Number of bytes in each value.
     */
    @Param("100")
    int valSize;

    static {
        RND.nextBytes(RND_MB);
        final String tmpParent = getProperty("java.io.tmpdir");
        TMP_BENCH = new File(tmpParent, "lmdbjava-benchmark-scratch");
    }

    public void setup(final BenchmarkParams b) throws IOException {
        keySize = intKey ? BYTES : STRING_KEY_LENGTH;
        crc = new CRC32();
        final IntHashSet set = new IntHashSet(num);
        keys = new int[num];
        for (int i = 0; i < num; i++) {
            if (sequential) {
                keys[i] = i;
            } else {
                while (true) {
                    int candidateKey = RND.nextInt();
                    if (candidateKey < 0) {
                        candidateKey *= -1;
                    }
                    if (!set.contains(candidateKey)) {
                        set.add(candidateKey);
                        keys[i] = candidateKey;
                        break;
                    }
                }
            }
        }

        rmdir(TMP_BENCH);
        tmp = create(b, "");
        compact = create(b, "-compacted");
    }

    public void reportSpaceBeforeClose() {
        if (tmp.getName().contains(".readKey-")) {
            reportSpaceUsed(tmp, "before-close");
        }
    }

    public void teardown() throws IOException {
        // we only output for key, as all impls offer it and it should be fixed
        if (tmp.getName().contains(".readKey-")) {
            reportSpaceUsed(tmp, "after-close");
        }
        rmdir(TMP_BENCH);
    }


    protected void reportSpaceUsed(final File dir, final String desc) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        long bytes = 0;
        for (final File f : files) {
            if (f.isDirectory()) {
                throw new UnsupportedOperationException("impl created directory");
            }
            final FileStat stat = POSIX.stat(f.getAbsolutePath());
            bytes += stat.blocks() * S_BLKSIZE;
        }
        out.println("\nBytes\t" + desc + "\t" + bytes + "\t" + dir.getName());
    }

    final String padKey(final int key) {
        final String skey = Integer.toString(key);
        return "0000000000000000".substring(0, 16 - skey.length()) + skey;
    }

    private File create(final BenchmarkParams b, final String suffix) {
        final File f = new File(TMP_BENCH, b.id() + suffix);
        if (!f.mkdirs()) {
            throw new IllegalStateException("Cannot mkdir " + f);
        }
        return f;
    }


    private void rmdir(final File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (final File f : files) {
                rmdir(f);
            }
        }
        if (!file.delete()) {
            throw new IllegalStateException("Cannot delete " + file);
        }
    }
}
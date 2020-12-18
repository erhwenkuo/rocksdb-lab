/**
 * Copyright (c) 2011-present, Facebook, Inc.  All rights reserved.
 *  This source code is licensed under both the GPLv2 (found in the
 *  COPYING file in the root directory) and Apache 2.0 License
 *  (found in the LICENSE.Apache file in the root directory).
 */
package io.github.erhwenkuo.rocksdb.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class FileUtils {
    private static final SimpleFileVisitor<Path> DELETE_DIR_VISITOR = new DeleteDirVisitor();

    /**
     * Delete a path from the filesystem
     *
     * If the path is a directory its contents
     * will be recursively deleted before it itself
     * is deleted.
     *
     * Note that removal of a directory is not an atomic-operation
     * and so if an error occurs during removal, some of the directories
     * descendants may have already been removed
     *
     * @param path the path to delete.
     * @throws IOException if an error occurs while removing a file or directory
     */
    public static void delete(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            Files.deleteIfExists(path);
        } else {
            Files.walkFileTree(path, DELETE_DIR_VISITOR);
        }
    }

    private static class DeleteDirVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes basicFileAttributes) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
            if (e != null)
                throw e;

            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }
    }
}

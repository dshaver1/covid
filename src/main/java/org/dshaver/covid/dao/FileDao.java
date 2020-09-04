package org.dshaver.covid.dao;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface FileDao<T> {
    /**
     * The path to the directory where files will be stored.
     */
    Path getPath();

    T save(T entity);

    List<T> findAll();

    Optional<T> findById(String id);

    void delete(T entity);

    void deleteAll();
}

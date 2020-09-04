package org.dshaver.covid.dao;

import java.util.List;
import java.util.Optional;

public abstract class BaseFileDao<T> implements FileDao<T> {
    @Override
    public T save(T entity) {
        return null;
    }

    @Override
    public List<T> findAll() {
        return null;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.empty();
    }

    @Override
    public void delete(T entity) {

    }

    @Override
    public void deleteAll() {

    }
}

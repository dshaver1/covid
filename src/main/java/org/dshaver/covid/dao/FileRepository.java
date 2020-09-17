package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.FileIndex;
import org.dshaver.covid.domain.Identifiable;
import org.dshaver.covid.service.FileRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

public interface FileRepository<T extends Identifiable> {
    /**
     * The path to the directory where files will be stored.
     */
    Path getPath();

    /**
     * The filename pattern to use
     */
    String createFilename(T entity);

    /**
     * The class of the entity we will deserialize into
     */
    Class<T> getClazz();

    T save(T entity) throws IOException;

    Stream<T> findAll();

    Optional<T> findById(String id);

    Optional<T> findByReportDate(LocalDate id);

    void delete(T entity);

    void deleteAll();

    FileIndex scanDirectory();

    Stream<T> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate);

    Stream<T> findByReportDateOrderByIdAsc(LocalDate reportDate);

    Stream<T> findAllByOrderByIdAsc();

    ObjectMapper getObjectMapper();

    FileRegistry getFileRegistry();

    default T readFile(Path path) throws IOException {
        return getObjectMapper().readValue(path.toFile(), getClazz());
    }

    default Stream<Path> streamAllPaths() throws IOException {
        return Files.list(getPath());
    }

    /**
     * Streams the selected entities for the day. (Some days have more than 1 report... we only care about the first one.)
     * @return
     */
    default Stream<Path> streamSelectedPaths() {
        return getFileRegistry().getPathsByReportDate(getClazz());
    }
}

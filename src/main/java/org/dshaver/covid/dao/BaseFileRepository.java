package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.dshaver.covid.domain.FileIndex;
import org.dshaver.covid.domain.Identifiable;
import org.dshaver.covid.service.FileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Built in caching mechanism via {@link FileRegistry}.
 */
public abstract class BaseFileRepository<T extends Identifiable> implements FileRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseFileRepository.class);
    public static final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("uuuu-MM-dd").toFormatter();

    private final ObjectMapper objectMapper;
    private final FileRegistry fileRegistry;
    private final Path path;

    protected BaseFileRepository(ObjectMapper objectMapper, FileRegistry fileRegistry, Path path) {
        this.objectMapper = objectMapper;
        this.fileRegistry = fileRegistry;
        this.path = path;

        if (!getPath().toFile().exists()) {
            try {
                Files.createDirectories(this.path);
            } catch (IOException e) {
                logger.error("Error creating directory! " + this.path, e);
            }
        }

        fileRegistry.putIndex(this.getClazz(), new FileIndex(LocalDateTime.now(), getClazz()));
    }

    @Override
    public T save(T entity) throws IOException {
        Path path = Paths.get(getPath().toString(), createFilename(entity));

        try {
            objectMapper.writeValue(path.toFile(), entity);
            entity.setFilePath(path);
            fileRegistry.addEntity(entity);
        } catch (IOException e) {
            logger.error("Error saving " + entity.getClass() + " entity with id " + entity.getId(), e);
            throw e;
        }

        return entity;
    }

    @Override
    public FileIndex scanDirectory() {
        FileIndex fileIndex = new FileIndex(LocalDateTime.now(), getClazz());
        try {
            streamAll().forEach(entity -> {
                fileIndex.getIdToPath().put(entity.getId(), entity.getFilePath());
                fileIndex.getReportDateToPath().put(entity.getReportDate(), entity.getFilePath());
            });
        } catch (IOException e) {
            logger.error("Could not scan directory!", e);
        }

        return fileIndex;
    }

    @Override
    public Stream<T> findAll() {
        try {
            return streamAll();
        } catch (IOException e) {
            logger.error("Error listing " + getClazz() + " files in " + getPath(), e);
        }

        return Stream.empty();
    }

    @Override
    public Optional<T> findById(String id) {
        Optional<Path> regFile = fileRegistry.getPath(getClazz(), id);
        Optional<T> entity = Optional.empty();

        try {
            if (regFile.isPresent()) {
                // Cache hit! Read file directly without searching.
                entity = Optional.ofNullable(objectMapper.readValue(regFile.get().toFile(), getClazz()));
            } else {
                // Cache miss. Look on disk.
                return streamAll().filter(o -> o.getId().equals(id)).findFirst();
            }
        } catch (IOException e) {
            logger.error("Error listing " + getClazz() + " files in " + getPath(), e);
        }

        return entity;
    }

    @Override
    public Optional<T> findByReportDate(LocalDate reportDate) {
        Optional<Path> regFile = fileRegistry.getPath(getClazz(), reportDate);
        Optional<T> entity = Optional.empty();

        try {
            if (regFile.isPresent()) {
                // Cache hit! Read file directly without searching.
                entity = Optional.ofNullable(objectMapper.readValue(regFile.get().toFile(), getClazz()));
            } else {
                // Cache miss. Look on disk.
                return streamAll().filter(o -> o.getReportDate().equals(reportDate)).findFirst();
            }
        } catch (IOException e) {
            logger.error("Error listing " + getClazz() + " files in " + getPath(), e);
        }

        return entity;
    }

    @Override
    public Stream<T> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate) {
        return Stream.empty();
    }

    @Override
    public Stream<T> findByReportDateOrderByIdAsc(LocalDate reportDate) {
        return Stream.empty();
    }

    @Override
    public Stream<T> findAllByOrderByIdAsc() {
        return Stream.empty();
    }

    private Stream<T> streamAll() throws IOException {
        return Files.list(getPath())
                .map(path -> {
                    try {
                        T entity = objectMapper.readValue(path.toFile(), getClazz());
                        entity.setFilePath(path);

                        return objectMapper.readValue(path.toFile(), getClazz());
                    } catch (Exception e) {
                        logger.error("Error reading " + getClazz() + " files in " + getPath(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public void delete(T entity) {
        try {
            Path path = Paths.get(getPath().toString(), createFilename(entity));
            if (path.toFile().exists()) {
                Files.delete(path);
                fileRegistry.removeEntity(entity);
            } else {
                logger.info("Tried to delete file {} but it doesn't exist!", path);
            }
        } catch (IOException e) {
            logger.info("Could not delete entity " + entity.getFilePath(), e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            FileUtils.cleanDirectory(getPath().toFile());
            fileRegistry.removeAll(getClazz());
        } catch (IOException e) {
            logger.info("Could not delete " + getClazz() + " in path " + getPath(), e);
        }
    }

    @Override
    public Path getPath() {
        return this.path;
    }
}

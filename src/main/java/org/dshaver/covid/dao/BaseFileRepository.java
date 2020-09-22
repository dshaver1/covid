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
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Built in caching mechanism via {@link FileRegistry}.
 */
public abstract class BaseFileRepository<T extends Identifiable> implements FileRepository<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseFileRepository.class);
    public static final Pattern reportDateFilenamePattern = Pattern.compile(".*(\\d{4}-\\d{2}-\\d{2})");
    public static final Pattern idFilenamePattern = Pattern.compile(".*(\\d{4}-\\d{2}-\\d{2}T\\d{6})");
    public static final DateTimeFormatter reportDateFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("uuuu-MM-dd").toFormatter();
    public static final DateTimeFormatter idFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("uuuu-MM-dd'T'HHmmss").toFormatter();

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

        fileRegistry.putIndex(this.getClazz(), scanDirectory());
    }

    @Override
    public T save(T entity) throws IOException {
        logger.info("Saving all {} entity with id {}...", getClazz(), entity.getId());
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
        logger.info("Scanning directory for {}: {}", this.getClazz(), this.getPath().toString());

        FileIndex fileIndex = new FileIndex(LocalDateTime.now(), getClazz());
        try {
            Files.list(getPath()).forEach(path -> {
                String id = getIdFromFilename(path);
                LocalDate reportDate = getReportDateFromFilename(path);

                fileIndex.getIdToPath().put(id, path);
                fileIndex.getReportDateToPath().put(reportDate, path);
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
        logger.info("Retrieving {} entity with id {}...", getClazz(), id);
        Optional<Path> regFile = fileRegistry.getPath(getClazz(), id);
        Optional<T> entity = Optional.empty();

        try {
            if (regFile.isPresent()) {
                // Cache hit! Read file directly without searching.
                entity = Optional.ofNullable(objectMapper.readValue(regFile.get().toFile(), getClazz()));
            } else {
                // Cache miss. Look on disk.
                return streamAllPaths()
                        .filter(path -> Objects.equals(getIdFromFilename(path), id))
                        .map(this::readFile)
                        .findFirst();
            }
        } catch (IOException e) {
            logger.error("Error listing " + getClazz() + " files in " + getPath(), e);
        }

        entity.ifPresent($ -> logger.info("Found {} entity with id {}...", getClazz(), $.getId()));

        return entity;
    }

    @Override
    public Optional<T> findByReportDate(LocalDate reportDate) {
        logger.info("Retrieving the {} entity for report date {}...", getClazz(), reportDate);
        Optional<Path> regFile = fileRegistry.getPath(getClazz(), reportDate);
        Optional<T> entity = Optional.empty();

        if (regFile.isPresent()) {
            // Cache hit! Read file directly without searching.
            entity = Optional.ofNullable(readFile(regFile.get()));
        } else {
            // Cache miss. Look on disk.
            return streamAllPaths()
                    .filter(path -> Objects.equals(getReportDateFromFilename(path), reportDate))
                    .map(this::readFile)
                    .findFirst();
        }

        return entity;
    }

    public Optional<T> getLatest() {
        logger.info("Retrieving latest {} entity...", getClazz());
        Optional<T> latest = Optional.empty();
        Optional<String> latestId = fileRegistry.getLatestId(getClazz());

        if (latestId.isPresent()) {
            Optional<Path> regFile = fileRegistry.getPath(getClazz(), latestId.get());
            if (regFile.isPresent()) {
                latest = Optional.ofNullable(readFile(regFile.get()));
            }
        }

        return latest;
    }

    @Override
    public Stream<T> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate) {
        return streamAllPaths()
                .filter(path -> {
                    LocalDate reportDate = getReportDateFromFilename(path.getFileName());
                    return !(reportDate.isBefore(startDate) || reportDate.isAfter(endDate));
                })
                .map(this::readFile)
                .sorted(Comparator.comparing(Identifiable::getId));
    }

    @Override
    public Stream<T> findByReportDateOrderByIdAsc(LocalDate reportDate) {
        return Stream.empty();
    }

    @Override
    public Stream<T> findAllByOrderByIdAsc() {
        return Stream.empty();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public FileRegistry getFileRegistry() {
        return fileRegistry;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private Stream<T> streamAll() throws IOException {
        logger.info("Retrieving all {} entities...", getClazz());
        return streamAllPaths()
                .map(this::readFile)
                .filter(Objects::nonNull);
    }

    @Override
    public void delete(T entity) {
        logger.info("Deleting {} entity with id {}", getClazz(), entity.getId());
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
        logger.info("Deleting all {} entities...", getClazz());
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

    private String getIdFromFilename(Path path) {
        Matcher matcher = idFilenamePattern.matcher(path.getFileName().toString());

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private LocalDate getReportDateFromFilename(Path path) {
        Matcher matcher = reportDateFilenamePattern.matcher(path.getFileName().toString());

        if (matcher.find()) {
            return LocalDate.parse(matcher.group(1), reportDateFormatter);
        } else {
            return null;
        }
    }
}

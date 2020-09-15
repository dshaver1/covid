package org.dshaver.covid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.FileIndex;
import org.dshaver.covid.domain.Identifiable;
import org.dshaver.covid.domain.MultiFileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Holds indices linking common attributes to their corresponding file paths so I don't have to scan through all files every time.
 */
@Service
public class FileRegistry {
    private static final Logger logger = LoggerFactory.getLogger(FileRegistry.class);

    private final ObjectMapper objectMapper;
    private final String indexPath;
    private final MultiFileIndex fileIndex;

    @Inject
    public FileRegistry(ObjectMapper objectMapper,
                        @Value("${covid.index.path}") String indexPath,
                        @Value("${covid.dirs.raw.v2}") String rawDir) {
        this.objectMapper = objectMapper;
        this.indexPath = indexPath;
        Optional<MultiFileIndex> fileIndex = Optional.empty();
        try {
            if (!Files.exists(Paths.get(rawDir))) {
                logger.info("Directory {} does not exist. Creating it.", rawDir);
                Files.createDirectories(Paths.get(rawDir));
            }

            fileIndex = readIndex();

            if (!fileIndex.isPresent()) {
                logger.info("File index {} does not exist. Scanning directories and creating index...", indexPath);
                Files.createDirectories(Paths.get(indexPath).getParent());
            }

        } catch (IOException e) {
            logger.error("Error creating directory! " + rawDir, e);
        }

        this.fileIndex = fileIndex.orElse(new MultiFileIndex(LocalDateTime.now()));
    }

    /**
     * Save new index to disk if the index in memory has been updated more recently than the one on disk.
     */
    @Scheduled(cron = "0 30 * * * *")
    public void checkAndSaveIndex() {
        Optional<MultiFileIndex> diskIndex = readIndex();

        if (diskIndex.isPresent()) {
            if (diskIndex.get().getLastUpdated().isBefore(this.fileIndex.getLastUpdated())) {
                writeIndex(this.fileIndex);
            }
        } else {
            writeIndex(this.fileIndex);
        }
    }

    public FileIndex getIndex(Class<? extends Identifiable> clazz) {
        FileIndex fileIndex = this.fileIndex.getMultimap().get(clazz);
        if (fileIndex == null) {
            fileIndex = new FileIndex(LocalDateTime.now(), clazz);
            putIndex(clazz, fileIndex);
        }

        return fileIndex;
    }

    public boolean isKnown(Class<? extends Identifiable> clazz, Path path) {
        return getIndex(clazz).getIdToPath().containsValue(path);
    }

    public Optional<String> getLatestId(Class<? extends Identifiable> clazz) {
        return getIndex(clazz).getIdToPath().keySet().stream().max(Comparator.naturalOrder());
    }

    public void putIndex(Class<? extends Identifiable> clazz, FileIndex fileIndex) {
        this.fileIndex.getMultimap().put(clazz, fileIndex);
        this.fileIndex.setLastUpdated(LocalDateTime.now());
    }

    public void addEntity(Identifiable entity) {
        addEntity(entity.getClass(), entity);
    }

    public void addEntity(Class<? extends Identifiable> clazz, Identifiable entity) {
        this.fileIndex.getMultimap().get(clazz).add(entity);
        this.fileIndex.setLastUpdated(LocalDateTime.now());
    }

    public void removeEntity(Identifiable entity) {
        this.fileIndex.getMultimap().get(entity.getClass()).remove(entity);
        this.fileIndex.setLastUpdated(LocalDateTime.now());
    }

    public void removeAll(Class<? extends Identifiable> clazz) {
        putIndex(clazz, new FileIndex(LocalDateTime.now(), clazz));
    }

    public Map<LocalDate, Path> getAllPaths(Class<? extends Identifiable> clazz) {
        return getIndex(clazz).getReportDateToPath();
    }

    public Optional<Path> getPath(Class<? extends Identifiable> clazz, String id) {
        return Optional.ofNullable(getIndex(clazz).getIdToPath().get(id));
    }

    public Optional<Path> getPath(Class<? extends Identifiable> clazz, LocalDate reportDate) {
        return Optional.ofNullable(getIndex(clazz).getReportDateToPath().get(reportDate));
    }

    private Optional<MultiFileIndex> readIndex() {
        Optional<MultiFileIndex> index = Optional.empty();

        try {
            index = Optional.ofNullable(objectMapper.readValue(new File(indexPath), MultiFileIndex.class));
        } catch (IOException e) {
            logger.error("Error reading index file " + indexPath, e);
        }

        if (index.isPresent()) {
            logger.info("Found index file {}!", indexPath);
        }

        return index;
    }

    private void writeIndex(MultiFileIndex index) {
        try {
            objectMapper.writeValue(Paths.get(indexPath).toFile(), index);
        } catch (IOException e) {
            logger.error("Error writing index file " + indexPath, e);
        }
    }
}

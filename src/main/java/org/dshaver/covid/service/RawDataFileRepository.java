package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RawDataFileRepository {
    private static final Logger logger = LoggerFactory.getLogger(RawDataFileRepository.class);
    public static final String filenamePrefix = "DPH_RAW_";
    public static final Pattern timePattern = Pattern.compile(filenamePrefix + "(\\d{4}-\\d{2}-\\d{2}T\\d{6})\\.js");
    public static final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("uuuu-MM-dd'T'HHmmss").toFormatter();

    private final RawDataDownloader2 rawDataDownloader2;
    private final String rawDir;

    @Inject
    public RawDataFileRepository(
            RawDataDownloader2 rawDataDownloader2,
            @Value("${covid.dirs.raw}") String rawDir) {
        this.rawDataDownloader2 = rawDataDownloader2;
        this.rawDir = rawDir;
    }

    public List<RawData> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate) {
        List<File> files = getRawDataFiles(startDate, endDate);
        return files.stream()
                .map(file -> {
                    try {
                        logger.info("Going to parse file: " + file.getName());
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        logger.error("Error reading file... " + file.getName(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(file -> rawDataDownloader2.transform(file, false))
                .sorted(Comparator.comparing(RawDataV2::getReportDate))
                .collect(Collectors.toList());
    }

    public File getLatestRawDataFile() {
        List<File> fileList = getAllRawDataFiles();

        return fileList.get(fileList.size()-1);
    }

    public List<File> getAllRawDataFiles() {
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2021, 1, 1);

        return getRawDataFiles(startDate, endDate);
    }

    public List<File> getRawDataFiles(LocalDate startDate, LocalDate endDate) {
        try {
            Map<LocalDate, List<Path>> groupedFiles = Files.list(Paths.get(rawDir))
                    .collect(Collectors.groupingBy(path -> {
                        String filename = path.getFileName().toString();
                        Matcher matcher = timePattern.matcher(filename);

                        if (matcher.matches()) {
                            String extractDate = matcher.group(1);
                            LocalDateTime fileDate = LocalDateTime.from(timeFormatter.parse(extractDate));

                            return fileDate.toLocalDate();
                        } else {
                            logger.debug("Skipping unrecognized file: " + filename);

                            return LocalDate.of(1990,1,1);
                        }
                    }));

            List<File> filteredFiles = new ArrayList<>();
            for (Map.Entry<LocalDate, List<Path>> entry : groupedFiles.entrySet()) {
                LocalDate fileDate = entry.getKey();
                if (!startDate.isAfter(fileDate) && !endDate.isBefore(fileDate)) {
                    for (int idx = 0; idx < entry.getValue().size(); idx++) {
                        Path path = entry.getValue().get(idx);
                        boolean lastElement = idx + 1 >= entry.getValue().size();
                        String filename = path.getFileName().toString();
                        Matcher matcher = timePattern.matcher(filename);

                        if (matcher.matches()) {
                            String extractDate = matcher.group(1);
                            if (is1800(extractDate) || lastElement) {
                                logger.info("Adding " + path.getFileName().toString() + "...");
                                filteredFiles.add(path.toFile());
                                break;
                            } else {
                                logger.debug("Skipping " + path.getFileName().toString() + " since there is a later one for this date...");
                            }
                        }
                    }
                }
            }

            filteredFiles.sort(Comparator.comparing(File::getName));

            return filteredFiles;
        } catch (IOException e) {
            throw new RuntimeException("Error getting file list from " + rawDir, e);
        }
    }

    private boolean is1800(String dateString) {
        int diff = 18 - LocalDateTime.parse(dateString, timeFormatter).getHour();

        return diff <= 0;
    }
}

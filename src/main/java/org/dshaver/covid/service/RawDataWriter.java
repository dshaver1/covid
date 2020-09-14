package org.dshaver.covid.service;

import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV1;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.dshaver.covid.service.RawDataFileRepository.filenamePrefix;
import static org.dshaver.covid.service.RawDataFileRepository.timeFormatter;

@Component
public class RawDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(RawDataWriter.class);

    private final RawDataRepositoryV1 rawDataRepositoryV1;
    private final RawDataRepositoryV2 rawDataRepositoryV2;
    private final ManualRawDataRepository manualRawDataRepository;
    private final String rawDir;

    @Inject
    public RawDataWriter(RawDataRepositoryV1 rawDataRepositoryV1,
                         RawDataRepositoryV2 rawDataRepositoryV2,
                         ManualRawDataRepository manualRawDataRepository,
                         @Value("${covid.dirs.raw.v2}") String rawDir) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.manualRawDataRepository = manualRawDataRepository;
        this.rawDir = rawDir;
    }

    public void write(RawData rawData) {
        write(filenamePrefix + rawData.getId().replace(":","") + ".js", rawData.getPayload());
    }

    public void write(String filename, List<String> lines) {
        try {
            Files.write(Paths.get(rawDir, filename), lines, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Could not write raw data file " + filename, e);
        }
    }

    public void write(LocalDateTime reportTime, List<String> lines) {
        write(filenamePrefix + timeFormatter.format(reportTime) + ".js", lines);
    }

    public void exportAllData() throws Exception {
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2021, 1, 1);

        exportDataBetweenDates(startDate, endDate);
    }

    public void exportDataBetweenDates(LocalDate startDate, LocalDate endDate) throws Exception {
        TreeSet<RawData> rawData = new TreeSet<>(Comparator.comparing(RawData::getId));

        rawData.addAll(rawDataRepositoryV1.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList()));
        rawData.addAll(rawDataRepositoryV2.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList()));
        rawData.addAll(manualRawDataRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList()));

        for (RawData currentData : rawData) {
            if (currentData.getPayload().size() > 1) {
                String filename = "DPH_RAW_" + currentData.getId().replace(":", "") + getExtension(currentData);
                Path path = Paths.get(rawDir).resolve(filename);
                Files.deleteIfExists(path);
                BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);

                currentData.getPayload().forEach(line -> {
                    try {
                        writer.write(line);
                    } catch (IOException e) {
                        logger.error("Error writing raw data file!", e);
                    }
                });

                writer.close();
            }
        }
    }

    private String getExtension(RawData data) {
        if (data instanceof RawDataV1) {
            return ".html";
        } else {
            return ".js";
        }
    }
}

package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.domain.BasicFile;
import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.service.RawDataFileRepository;
import org.dshaver.covid.service.RawDataWriter;
import org.dshaver.covid.service.ReportService;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl1;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.dshaver.covid.dao.BaseFileRepository.idFormatter;

/**
 * Created by xpdf64 on 2020-06-12.
 */
@RestController
public class RawDataController {
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataWriter rawDataWriter;
    private final RawDataFileRepository fileRepository;
    private final EpicurveExtractorImpl1 extractorImpl1;
    private final EpicurveExtractorImpl2 extractorImpl2;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;

    @Inject
    public RawDataController(RawDataWriter rawDataWriter,
                             RawDataFileRepository fileRepository,
                             EpicurveExtractorImpl1 extractorImpl1,
                             EpicurveExtractorImpl2 extractorImpl2,
                             ObjectMapper objectMapper,
                             ReportService reportService) {
        this.rawDataWriter = rawDataWriter;
        this.fileRepository = fileRepository;
        this.extractorImpl1 = extractorImpl1;
        this.extractorImpl2 = extractorImpl2;
        this.objectMapper = objectMapper;
        this.reportService = reportService;
    }

    @PostMapping("/covid/api/poll")
    public DownloadResponse checkForData() throws Exception {
        return reportService.checkForData();
    }

    @PostMapping("/covid/api/reprocess")
    public void reprocess(@RequestParam(name = "startDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(name = "endDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          @RequestParam(name = "clean", required = false) Boolean clean) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);
        boolean defaultedClean = clean == null ? true : clean;

        reportService.processRange(defaultedStartDate, defaultedEndDate, defaultedClean);
    }

    @PostMapping("/covid/api/transformRaw")
    public void transformRaw() throws Exception {
        String v1PathString = "H:\\dev\\workingdir\\covid\\raw\\temp";
        String v2PathString = "H:\\dev\\workingdir\\covid\\raw\\v2";
        Path outputDir = Paths.get("H:\\dev\\workingdir\\covid\\raw\\v0");
        List<Path> paths = Streams.concat(Files.list(Paths.get(v1PathString)))
                //, Files.list(Paths.get(v2PathString)))
                .collect(Collectors.toList());

        for (Path path : paths) {
            String originalFilename = path.getFileName().toString();
            String outputFilename = originalFilename.substring(0, originalFilename.indexOf("."));
            Path fullOutputPath = outputDir.resolve(outputFilename + ".json");

            List<String> allLines = Files.readAllLines(path);
            try (BufferedWriter writer = Files.newBufferedWriter(fullOutputPath)) {
                String id = outputFilename.substring(outputFilename.indexOf("2"));

                LocalDate reportDate = LocalDateTime.parse(id, idFormatter).toLocalDate();

                BasicFile file = new BasicFile(reportDate, id, fullOutputPath, allLines);

                objectMapper.writeValue(writer, file);
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

package org.dshaver.covid.controller;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV1;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.service.RawDataFileRepository;
import org.dshaver.covid.service.RawDataWriter;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl1;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.dshaver.covid.service.RawDataParsingTools.find;

/**
 * Created by xpdf64 on 2020-06-12.
 */
@RestController
public class RawDataController {
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataRepositoryV1 rawDataRepositoryV1;
    private final RawDataRepositoryV2 rawDataRepositoryV2;
    private final ManualRawDataRepository manualRawDataRepository;
    private final RawDataWriter rawDataWriter;
    private final RawDataFileRepository fileRepository;
    private final EpicurveExtractorImpl1 extractorImpl1;
    private final EpicurveExtractorImpl2 extractorImpl2;
    private final String rawDir;

    @Inject
    public RawDataController(RawDataRepositoryV1 rawDataRepositoryV1,
                             RawDataRepositoryV2 rawDataRepositoryV2,
                             ManualRawDataRepository manualRawDataRepository,
                             RawDataWriter rawDataWriter,
                             RawDataFileRepository fileRepository,
                             EpicurveExtractorImpl1 extractorImpl1, EpicurveExtractorImpl2 extractorImpl2, @Value("${covid.raw.dir}") String rawDir) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.manualRawDataRepository = manualRawDataRepository;
        this.rawDataWriter = rawDataWriter;
        this.fileRepository = fileRepository;
        this.extractorImpl1 = extractorImpl1;
        this.extractorImpl2 = extractorImpl2;
        this.rawDir = rawDir;
    }

    @GetMapping("/rawdata/metadata")
    public Collection<RawData> getRawDataMetadata(@RequestParam(name = "reportDate")
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        TreeSet<RawData> rawData = new TreeSet<>(Comparator.comparing(RawData::getId));

        rawData.addAll(fileRepository.findByReportDateBetweenOrderByIdAsc(reportDate, reportDate));

        return rawData;
    }

    @GetMapping("/rawdata")
    public String getRawData(@RequestParam(name = "reportDate")
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                             @RequestParam(name = "filter", required = false) String filter,
                             @RequestParam(name = "formatted", required = false) Boolean formatted) throws Exception {
        boolean defaultedFormatted = formatted == null ? true : formatted;
        logger.info("Got request for raw data: {} with filter {} ", reportDate, filter);

        Collection<File> files = fileRepository.getRawDataFiles(reportDate, reportDate);

        Preconditions.checkArgument(files.size() == 1, "Should have only found 1 raw data file for date " + reportDate);

        Path path = files.stream().map(File::toPath).findFirst().get();

        List<String> rawStrings = Files.readAllLines(path);

        if (filter != null && filter.equals("epicurve")) {
            Optional<String> epicurveString = find(rawStrings, extractorImpl2.getPattern());
            if (!epicurveString.isPresent()) {
                epicurveString = find(rawStrings, extractorImpl1.getPattern());
            }

            if (epicurveString.isPresent()) {
                if (defaultedFormatted) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonParser jp = new JsonParser();
                    JsonElement je = jp.parse(epicurveString.get());
                    String prettyJsonString = gson.toJson(je);
                    return prettyJsonString;
                }

                return epicurveString.get();
            }
        }


        return String.join("\n", rawStrings);
    }


    @PostMapping("/rawdata/writeFiles")
    public void writeRawData(@RequestParam(name = "startDate")
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             @RequestParam(name = "endDate")
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        rawDataWriter.exportAllData();
    }

    private String getExtension(RawData data) {
        if (data instanceof RawDataV1) {
            return ".html";
        } else {
            return ".js";
        }
    }
}

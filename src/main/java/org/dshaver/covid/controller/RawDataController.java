package org.dshaver.covid.controller;

import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV1;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.service.RawDataFileRepository;
import org.dshaver.covid.service.RawDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

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
    private final String rawDir;

    @Inject
    public RawDataController(RawDataRepositoryV1 rawDataRepositoryV1,
                             RawDataRepositoryV2 rawDataRepositoryV2,
                             ManualRawDataRepository manualRawDataRepository,
                             RawDataWriter rawDataWriter,
                             @Value("${covid.raw.dir}") String rawDir) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.manualRawDataRepository = manualRawDataRepository;
        this.rawDataWriter = rawDataWriter;
        this.rawDir = rawDir;
    }

    @GetMapping("/rawdata")
    public Collection<RawData> getRawData(@RequestParam(name = "reportDate")
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        TreeSet<RawData> rawData = new TreeSet<>(Comparator.comparing(RawData::getId));

        rawData.addAll(rawDataRepositoryV1.findByReportDateOrderByIdAsc(reportDate));
        rawData.addAll(rawDataRepositoryV2.findByReportDateOrderByIdAsc(reportDate));
        rawData.addAll(manualRawDataRepository.findByReportDateOrderByIdAsc(reportDate));

        return rawData;
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

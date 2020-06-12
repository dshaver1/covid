package org.dshaver.covid.controller;

import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV1;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.domain.RawData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Inject
    public RawDataController(RawDataRepositoryV1 rawDataRepositoryV1,
                             RawDataRepositoryV2 rawDataRepositoryV2,
                             ManualRawDataRepository manualRawDataRepository) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.manualRawDataRepository = manualRawDataRepository;
    }

    @GetMapping("/rawdata")
    public Collection<RawData> getReports(@RequestParam(name = "reportDate")
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        TreeSet<RawData> rawData = new TreeSet<>(Comparator.comparing(RawData::getId));

        rawData.addAll(rawDataRepositoryV1.findByReportDateOrderByIdAsc(reportDate));
        rawData.addAll(rawDataRepositoryV2.findByReportDateOrderByIdAsc(reportDate));
        rawData.addAll(manualRawDataRepository.findByReportDateOrderByIdAsc(reportDate));

        return rawData;
    }
}

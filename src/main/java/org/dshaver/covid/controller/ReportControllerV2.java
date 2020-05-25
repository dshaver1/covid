package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.dshaver.covid.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@RestController
public class ReportControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ReportControllerV2.class);
    private static final String REPORT_TGT_DIR = "H:\\dev\\covid\\src\\main\\resources\\static\\reports\\";
    private static final int DEFAULT_TARGET_HOUR = 18;
    private final ReportRepository reportRepository;
    private final RawDataRepositoryV2 rawDataRepository;
    private final ManualRawDataRepository manualRawDataRepository;
    private final HistogramReportRepository histogramReportRepository;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReportControllerV2(ReportRepository reportRepository,
                              RawDataRepositoryV2 rawDataRepository,
                              ManualRawDataRepository manualRawDataRepository,
                              HistogramReportRepository histogramReportRepository,
                              ReportService reportService,
                              ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.rawDataRepository = rawDataRepository;
        this.manualRawDataRepository = manualRawDataRepository;
        this.histogramReportRepository = histogramReportRepository;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reports/v2/daily")
    public Collection<ArrayReport> getReports(@RequestParam(name = "startDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(name = "endDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<ArrayReport> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate).stream().map(ArrayReport::new).collect(Collectors.toList());

        File file = Paths.get(REPORT_TGT_DIR, "v2daily").toFile();
        objectMapper.writeValue(file, reportList);

        return reportList;
    }

    @GetMapping("/reports/v2/latest")
    public ArrayReport getLatestReport() {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));
        reports.addAll(reportRepository.findAll());

        return new ArrayReport(reports.last());
    }
}

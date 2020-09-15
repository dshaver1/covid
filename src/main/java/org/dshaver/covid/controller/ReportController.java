package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.dshaver.covid.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@RestController
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private static final int DEFAULT_TARGET_HOUR = 18;
    private final ReportRepository reportRepository;
    private final RawDataRepositoryV2 rawDataRepository;
    private final ManualRawDataRepository manualRawDataRepository;
    private final HistogramReportRepository histogramReportDao;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;
    private final String reportTgtDir;

    @Inject
    public ReportController(@Value("${covid.dirs.reports.root}") String reportTgtDir,
                            ReportRepository reportRepository,
                            RawDataRepositoryV2 rawDataRepository,
                            ManualRawDataRepository manualRawDataRepository,
                            HistogramReportRepository histogramReportDao,
                            ReportService reportService,
                            ObjectMapper objectMapper) {
        this.reportTgtDir = reportTgtDir;
        this.reportRepository = reportRepository;
        this.rawDataRepository = rawDataRepository;
        this.manualRawDataRepository = manualRawDataRepository;
        this.histogramReportDao = histogramReportDao;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }
}

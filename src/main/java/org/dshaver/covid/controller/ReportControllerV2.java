package org.dshaver.covid.controller;

import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.ArrayReport;
import org.dshaver.covid.domain.CountyRankReport;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.service.AggregateReportFactory;
import org.dshaver.covid.service.CsvService;
import org.dshaver.covid.service.HistogramReportFactory;
import org.dshaver.covid.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@RestController
public class ReportControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ReportControllerV2.class);
    private static final int DEFAULT_TARGET_HOUR = 18;
    private final ReportRepository reportRepository;
    private final String reportTgtDir;
    private final CsvService csvService;
    private final HistogramReportFactory histogramReportFactory;
    private final ReportService reportService;
    private final AggregateReportFactory aggregateReportFactory;

    @Inject
    public ReportControllerV2(ReportRepository reportRepository,
                              @Value("${covid.dirs.reports.csv}") String reportTgtDir,
                              CsvService csvService,
                              HistogramReportFactory histogramReportFactory,
                              ReportService reportService,
                              AggregateReportFactory aggregateReportFactory) {
        this.reportRepository = reportRepository;
        this.reportTgtDir = reportTgtDir;
        this.csvService = csvService;
        this.histogramReportFactory = histogramReportFactory;
        this.reportService = reportService;
        this.aggregateReportFactory = aggregateReportFactory;
    }

    @GetMapping(value = "/covid/api/reports/v2/{file}.csv", produces = "text/csv")
    public String getCsv(@PathVariable(name = "file") String file) throws Exception {
        logger.info("Got request for csv file: " + file);

        switch (file) {
            case "cases":
                return csvService.readFile("cases.csv");
            case "caseDeltas":
                return csvService.readFile("caseDeltas.csv");
            case "caseProjections":
                return csvService.readFile("caseProjections.csv");
            case "movingAvgs":
                return csvService.readFile("movingAvgs.csv");
            case "deaths":
                return csvService.readFile("deaths.csv");
            case "deathDeltas":
                return csvService.readFile("deathDeltas.csv");
            case "summary":
                return csvService.readFile("summary.csv");
            default:
                throw new UnsupportedOperationException("Unrecognized filename request: " + file + ".csv");
        }
    }

    @GetMapping(value = "/covid/api/reports/v2/county/{type}_{county}.csv", produces = "text/csv")
    public String getCountyCsv(@PathVariable(name = "type") String type,
                               @PathVariable(name = "county") String county) throws Exception {
        logger.info("Got request for csv file: " + type + "_" + county);

        switch (type) {
            case "cases":
                return csvService.readFile("cases_" + county.toLowerCase() + ".csv");
            case "caseDeltas":
                return csvService.readFile("caseDeltas_" + county.toLowerCase() + ".csv");
            case "caseProjections":
                return csvService.readFile("caseProjections_" + county.toLowerCase() + ".csv");
            case "movingAvgs":
                return csvService.readFile("movingAvgs_" + county.toLowerCase() + ".csv");
            case "deaths":
                return csvService.readFile("deaths_" + county.toLowerCase() + ".csv");
            case "deathDeltas":
                return csvService.readFile("deathDeltas_" + county.toLowerCase() + ".csv");
            case "summary":
                return csvService.readFile("summary_" + county.toLowerCase() + ".csv");
            default:
                throw new UnsupportedOperationException("Unrecognized filename request: " + type + "_" + county + ".csv");
        }
    }


    @GetMapping("/covid/api/reports/v2/daily")
    public Collection<ArrayReport> getReports(@RequestParam(name = "startDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam(name = "endDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        logger.info("Got request for daily v2 report");

        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<ArrayReport> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate).map(ArrayReport::new).collect(Collectors.toList());

        //File file = Paths.get(REPORT_TGT_DIR, "daily").toFile();
        //objectMapper.writeValue(file, reportList);

        return reportList;
    }

    @GetMapping("/covid/api/reports/v2/latest")
    public ArrayReport getLatestReport() {
        logger.info("Got request for latest v2 report");
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));
        reports.addAll(reportRepository.findAll().collect(Collectors.toList()));

        return new ArrayReport(reports.last());
    }

    @GetMapping("/covid/api/reports/v2/countyRankByReportDate/{reportDate}")
    public CountyRankReport getCountyRankReport(@PathVariable("reportDate")
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
                                                @RequestParam(name = "prelim", required = false) String prelim) {
        boolean prelimToggle = prelim != null && "before".equals(prelim.toLowerCase());
        Optional<Report> maybeReport = reportRepository.findByReportDate(reportDate);

        logger.info("Building CountyRankReport for {} with prelimToggle set to {}", reportDate, prelimToggle);

        return maybeReport.map(report -> new CountyRankReport(report, prelimToggle)).orElseGet(CountyRankReport::new);
    }

    @PostMapping("/covid/api/reports/histogram/calculate")
    public void calculateHistogram(@RequestParam(name = "startDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(name = "endDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(name = "windowSize", required = false) Integer windowSize) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate;
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate;
        Integer defaultedWindowSize = windowSize == null ? 7 : windowSize;

        reportService.processHistogramRange(defaultedStartDate, defaultedEndDate, defaultedWindowSize);
    }

    @PostMapping("/covid/api/reports/histogram/csvs")
    public void generateHistogramCsvs(@RequestParam(name = "startDate", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                      @RequestParam(name = "endDate", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate;
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate;
        reportService.createHistogramCsvs(defaultedStartDate, defaultedEndDate);
    }

    @PostMapping("/covid/api/reports/aggregate/calculate")
    public void calculateAggregate(@RequestParam(name = "startDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(name = "endDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate;
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate;

        aggregateReportFactory.createAllAggregateReports(defaultedStartDate, defaultedEndDate);
    }
}

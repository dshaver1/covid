package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.RawDataRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.AggregateReport;
import org.dshaver.covid.domain.DownloadRequest;
import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@RestController
public class ReportController {
    private static final String REPORT_TGT_DIR = "H:\\dev\\covid\\src\\main\\resources\\static\\reports\\";
    private final ReportRepository reportRepository;
    private final RawDataRepository rawDataRepository;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReportController(ReportRepository reportRepository,
                            RawDataRepository rawDataRepository,
                            ReportService reportService,
                            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.rawDataRepository = rawDataRepository;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reports/daily")
    public Collection<Report> getReports(@RequestParam(name = "startDate", required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                     LocalDate startDate,
                                         @RequestParam(name = "endDate", required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020,1,1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030,1,1) : endDate.plusDays(1);

        List<Report> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate);

        Map<LocalDate, Report> reportMap = new HashMap<>();
        reportList.forEach(report -> reportMap.put(report.getReportDate(), report));

        TreeSet<Report> sorted = new TreeSet<>(Comparator.comparing(Report::getId));
        sorted.addAll(reportMap.values());

        File file = Paths.get(REPORT_TGT_DIR, "daily").toFile();
        objectMapper.writeValue(file, sorted);

        return sorted;
    }

    @GetMapping("/reports/aggregate")
    public AggregateReport aggregateReport(@RequestParam(name = "startDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                           LocalDate startDate,
                                           @RequestParam(name = "endDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020,1,1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030,1,1) : endDate.plusDays(1);
        List<Report> reports = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate);

        return new AggregateReport(reports);
    }

    @PostMapping("/reports/download")
    public List<DownloadResponse> download(@RequestBody DownloadRequest request) {
        return request.getUrls().stream().map(reportService::downloadData).collect(Collectors.toList());
    }

    @PostMapping("/reports/downloadLatest")
    public DownloadResponse downloadLatest() {
        return reportService.checkForData();
    }

    @GetMapping("/reports")
    public Collection<Report> getReports(@RequestParam(name = "reportDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));

        if (reportDate == null) {
            reports.addAll(reportRepository.findAll());
        } else {
            reports.addAll(reportRepository.findByReportDateOrderByIdAsc(reportDate));
        }

        return reports;
    }

    @GetMapping("/reports/latest")
    public Report getLatestReport() {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));
        reports.addAll(reportRepository.findAll());

        return reports.last();
    }

    @GetMapping("/reports/{id}")
    public Optional<Report> getReportById(@PathVariable("id") String id) {
        return reportRepository.findById(id);
    }

    @PostMapping("/reports/reprocess")
    public void reprocessAll() {
        reportService.bulkProcess(true);
    }
}

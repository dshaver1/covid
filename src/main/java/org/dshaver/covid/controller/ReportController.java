package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.opencsv.CSVWriter;
import org.dshaver.covid.dao.RawDataRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.service.ReportService;
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
public class ReportController {
    private static final String REPORT_TGT_DIR = "H:\\dev\\covid\\src\\main\\resources\\static\\reports\\";
    private static final int DEFAULT_TARGET_HOUR = 18;
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

    // TODO finish csv
    @GetMapping("/reports/dailyCsv")
    public void getCsvReports() throws Exception {
        Collection<Report> reports = getReports(null, null, null);

        String[] header = reports.stream()
                .reduce((first, second) -> second)
                .get().getEpicurve().getEpicurvePoints()
                .stream()
                .map(EpicurvePoint::getLabel)
                .collect(Collectors.toList())
                .toArray(new String[]{});

        Path path = Paths.get(REPORT_TGT_DIR);
        CSVWriter writer =
                new CSVWriter(Files.newBufferedWriter(path.resolve("daily.csv"), StandardOpenOption.CREATE_NEW));

        writer.writeNext(header);

        for (Report report : reports) {

        }

        writer.close();
    }

    @GetMapping("/reports/histogram")
    public HistogramReport getHistogramReport(@RequestParam(name = "startDate", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                          LocalDate startDate,
                                              @RequestParam(name = "endDate", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);
        Collection<Report> reports = getReports(defaultedStartDate, defaultedEndDate, 18);

        return new HistogramReport(reports);
    }

    @GetMapping("/reports/daily")
    public Collection<Report> getReports(@RequestParam(name = "startDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(name = "endDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(name = "targetHour", required = false) Integer targetHour) throws Exception {
        int defaultedTargetHour = targetHour == null ? DEFAULT_TARGET_HOUR : targetHour;
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<Report> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate);

        Multimap<LocalDate, Report> reportMap = ArrayListMultimap.create();
        reportList.forEach(report -> reportMap.put(report.getReportDate(), report));

        TreeSet<Report> sorted = new TreeSet<>(Comparator.comparing(Report::getId));

        for (LocalDate date : reportMap.keySet()) {
            sorted.add(getClosestReport(reportMap.get(date), defaultedTargetHour));
        }

        File file = Paths.get(REPORT_TGT_DIR, "daily").toFile();
        objectMapper.writeValue(file, sorted);

        return sorted;
    }

    private Report getClosestReport(Collection<Report> reports, int targetHour) {
        Report selected = reports.stream().findFirst().get();
        int bestDiff = Math.abs(LocalDateTime.parse(selected.getId(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).getHour() - targetHour);

        for (Report report : reports) {
            int currentDiff = Math.abs(LocalDateTime.parse(report.getId(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).getHour() - targetHour);

            if (currentDiff < bestDiff) {
                bestDiff = currentDiff;
                selected = report;
            }
        }

        return selected;
    }

    @GetMapping("/reports/aggregate")
    public AggregateReport aggregateReport(@RequestParam(name = "startDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate startDate,
                                           @RequestParam(name = "endDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);
        Collection<Report> reports = getReports(defaultedStartDate, defaultedEndDate, 18);

        return new AggregateReport(reports);
    }

    @PostMapping("/reports/download")
    public List<DownloadResponse> download(@RequestBody DownloadRequest request) {
        return request.getUrls().stream().map(reportService::downloadDataV1).collect(Collectors.toList());
    }

    @PostMapping("/reports/downloadLatest")
    public List<DownloadResponse> downloadLatest() {
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

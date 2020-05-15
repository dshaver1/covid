package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.dshaver.covid.dao.RawDataRepositoryV1;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
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
    private final RawDataRepositoryV1 rawDataRepository;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReportController(ReportRepository reportRepository,
                            RawDataRepositoryV1 rawDataRepository,
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
        Collection<Report> reports = getReports(null, null);

        String[] header = reports.stream()
                .reduce((first, second) -> second)
                .get().getGeorgiaEpicurve().getData()
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
        Collection<Report> reports = getReports(defaultedStartDate, defaultedEndDate);

        return new HistogramReport(reports);
    }

    @GetMapping("/reports/daily")
    public Collection<Report> getReports(@RequestParam(name = "startDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(name = "endDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<Report> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate);

        File file = Paths.get(REPORT_TGT_DIR, "daily").toFile();
        objectMapper.writeValue(file, reportList);

        return reportList;
    }

    @GetMapping("/reports/aggregate")
    public AggregateReport aggregateReport(@RequestParam(name = "startDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate startDate,
                                           @RequestParam(name = "endDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);
        Collection<Report> reports = getReports(defaultedStartDate, defaultedEndDate);

        return new AggregateReport(reports);
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
        LocalDate defaultStartDate = LocalDate.of(2020, 1, 1);
        LocalDate defaultEndDate = LocalDate.of(2030, 1, 1);
        reportService.bulkProcess(defaultStartDate, defaultEndDate, true, RawDataV1.class);
        // Don't delete data a second time because then we would never have any V1 reports
        reportService.bulkProcess(defaultStartDate, defaultEndDate, false, RawDataV2.class);
    }
}

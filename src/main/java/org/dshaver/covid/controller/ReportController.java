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
    public ReportController(@Value("${covid.dirs.report.target}") String reportTgtDir,
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

    @GetMapping("/covid/api/reports/histogram")
    public HistogramReport getHistogramReport(@RequestParam(name = "startDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam(name = "endDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {

        // Save histogram report object
        HistogramReport histogramReport = new HistogramReport(reportRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList()));
        try {
            logger.info("Saving histogram report for dates {} - {}.", startDate, endDate);
            histogramReportDao.save(histogramReport);
        } catch (Exception e) {
            logger.info("Already saved this histogram report. Skipping... ");
        }

        Collection<HistogramReport> reports = histogramReportDao.findAllByOrderByIdDesc();

        return reports.stream().findFirst().get();
    }

    @GetMapping("/covid/api/reports/daily")
    public Collection<Report> getReports(@RequestParam(name = "startDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(name = "endDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<Report> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate).collect(Collectors.toList());

        File file = Paths.get(reportTgtDir, "daily").toFile();
        objectMapper.writeValue(file, reportList);

        return reportList;
    }

    @GetMapping("/covid/api/reports/aggregate")
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

    @PostMapping("/covid/api/reports/downloadLatest")
    public DownloadResponse downloadLatest() {
        return reportService.checkForData();
    }

    @PostMapping("/covid/api/reports/copyRawData")
    public void downloadLatest(@RequestParam(name = "idToCopy") String idToCopy,
                               @RequestParam(name = "targetId") String targetId,
                               @RequestParam(name = "reportDate")
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) throws Exception {
        Optional<RawDataV2> rawDataV2 = rawDataRepository.findById(idToCopy);
        rawDataV2.get().setId(targetId);
        rawDataV2.get().setReportDate(reportDate);

        rawDataRepository.save(rawDataV2.get());
    }

    @GetMapping("/covid/api/reports")
    public Collection<Report> getReports(@RequestParam(name = "reportDate", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));

        if (reportDate == null) {
            reports.addAll(reportRepository.findAll().collect(Collectors.toList()));
        } else {
            reports.addAll(reportRepository.findByReportDateOrderByIdAsc(reportDate).collect(Collectors.toList()));
        }

        return reports;
    }

    @GetMapping("/covid/api/reports/latest")
    public Report getLatestReport() {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));
        reports.addAll(reportRepository.findAll().collect(Collectors.toList()));

        return reports.last();
    }

    @GetMapping("/covid/api/reports/{id}")
    public Optional<Report> getReportById(@PathVariable("id") String id) {
        return reportRepository.findById(id);
    }

    @PostMapping("/covid/api/reports/reprocess")
    public void reprocessAll() {
        LocalDate defaultStartDate = LocalDate.of(2020, 1, 1);
        LocalDate defaultEndDate = LocalDate.of(2030, 1, 1);
        reportService.bulkProcess(defaultStartDate, defaultEndDate, true);
        // Don't delete data a second time because then we would never have any V1 reports
        //reportService.bulkProcess(defaultStartDate, defaultEndDate, false, RawDataV2.class);
    }

    @PostMapping("/covid/api/reports/insertManualData")
    public void insertManualData(@RequestBody List<ManualReportRequest> requests) throws Exception {
        LocalDate defaultStartDate = LocalDate.of(2020, 1, 1);
        LocalDate defaultEndDate = LocalDate.of(2030, 1, 1);

        for (ManualReportRequest request : requests) {
            // Populate epicurve points with top-level reportDate
            for (EpicurvePoint epicurvePoint : request.getGeorgiaEpicurve()) {
                epicurvePoint.setLabelDate(LocalDate.parse(epicurvePoint.getTestDate(), DateTimeFormatter.ISO_DATE));
                epicurvePoint.setLabel(epicurvePoint.getTestDate());
            }

            // Convert to string
            String epicurveString = objectMapper.writeValueAsString(request.getGeorgiaEpicurve());

            // Create ManualRawData
            ManualRawData manualRawData = new ManualRawData();
            manualRawData.setId(request.getId());
            manualRawData.setReportDate(request.getReportDate());
            manualRawData.setCreateTime(LocalDateTime.now());
            manualRawData.setPayload(Collections.singletonList(epicurveString));
            manualRawData.setConfirmedCases(request.getConfirmedCases());
            manualRawData.setTotalTests(request.getTotalTests());
            manualRawData.setIcu(request.getIcu());
            manualRawData.setHospitalizations(request.getHospitalizations());
            manualRawData.setDeaths(request.getDeaths());

            // Save the raw data
            manualRawDataRepository.save(manualRawData);
        }

        // Reprocess all data into reports
        reportService.bulkProcess(defaultStartDate, defaultEndDate, true);
    }
}

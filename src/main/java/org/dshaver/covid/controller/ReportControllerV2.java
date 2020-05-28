package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ManualRawDataRepository;
import org.dshaver.covid.dao.RawDataRepositoryV2;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.ArrayReport;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@RestController
public class ReportControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ReportControllerV2.class);
    private static final int DEFAULT_TARGET_HOUR = 18;
    private final ReportRepository reportRepository;
    private final RawDataRepositoryV2 rawDataRepository;
    private final ManualRawDataRepository manualRawDataRepository;
    private final HistogramReportRepository histogramReportRepository;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;
    private final String reportTgtDir;

    @Inject
    public ReportControllerV2(ReportRepository reportRepository,
                              RawDataRepositoryV2 rawDataRepository,
                              ManualRawDataRepository manualRawDataRepository,
                              HistogramReportRepository histogramReportRepository,
                              ReportService reportService,
                              ObjectMapper objectMapper,
                              @Value("${covid.report.target.v2.dir}") String reportTgtDir) {
        this.reportRepository = reportRepository;
        this.rawDataRepository = rawDataRepository;
        this.manualRawDataRepository = manualRawDataRepository;
        this.histogramReportRepository = histogramReportRepository;
        this.reportService = reportService;
        this.objectMapper = objectMapper;
        this.reportTgtDir = reportTgtDir;
    }

    @CrossOrigin(origins = "http://rectangular-deposit.glitch.me")
    @GetMapping(value = "/reports/v2/{file}.csv", produces = "text/csv")
    public String getCsv(@PathVariable(name = "file") String file) throws Exception {
        switch (file) {
            case "cases":
                return readFile("cases.csv");
            case "caseDeltas":
                return readFile("caseDeltas.csv");
            case "caseProjections":
                return readFile("caseProjections.csv");
            case "movingAvgs":
                return readFile("movingAvgs.csv");
            case "deaths":
                return readFile("deaths.csv");
            case "deathDeltas":
                return readFile("deathDeltas.csv");
            case "summary":
                return readFile("summary.csv");
            default:
                throw new UnsupportedOperationException("Unrecognized filename request: " + file + ".csv");
        }
    }


    @PostMapping("/reports/v2/generateAllCsvs")
    public void generateCsv() throws Exception {
        Collection<ArrayReport> reports = getReports(null, null);

        String[] headerArray = createHeader(reports);

        writeFile("cases.csv", headerArray, reports, ArrayReport::getCases);
        writeFile("caseDeltas.csv", headerArray, reports, ArrayReport::getCaseDeltas);
        writeFile("caseProjections.csv", headerArray, reports, ArrayReport::getCaseProjections);
        writeFile("movingAvgs.csv", headerArray, reports, ArrayReport::getMovingAvgs);
        writeFile("deaths.csv", headerArray, reports, ArrayReport::getDeaths);
        writeFile("deathDeltas.csv", headerArray, reports, ArrayReport::getDeathDeltas);
        writeSummary("summary.csv", reports);
    }

    private String[] createHeader(Collection<ArrayReport> reports) {
        List<String> header = new ArrayList<>();
        header.add("id");
        header.addAll(Arrays.stream(reports.stream()
                .skip(reports.size() - 1)
                .findFirst()
                .get()
                .getCurveDates())
                .map(LocalDate::toString)
                .collect(Collectors.toList()));

        return header.toArray(new String[]{});
    }

    private String writeSummary(String filename, Collection<ArrayReport> reports) throws Exception {
        Path path = Paths.get(reportTgtDir).resolve(filename);
        CSVWriter writer =
                new CSVWriter(Files.newBufferedWriter(path, StandardOpenOption.CREATE));

        String[] header = new String[]{"id", "createTime", "reportDate", "totalTests", "totalTestVm",
                "totalConfirmedCases", "totalDeaths", "confirmedCasesVm", "hospitalized", "hospitalizedVm", "deathsVm",
                "icu", "icuVm"};

        writer.writeNext(header);

        List<String> row = new ArrayList<>();
        for (ArrayReport report : reports) {
            row.add(report.getId());
            row.add(report.getCreateTime().toString());
            row.add(report.getReportDate().toString());
            row.add("" + report.getTotalTests());
            row.add("" + report.getTotalTestsVm());
            row.add("" + report.getTotalConfirmedCases());
            row.add("" + report.getTotalDeaths());
            row.add("" + report.getConfirmedCasesVm());
            row.add("" + report.getHospitalized());
            row.add("" + report.getHospitalizedVm());
            row.add("" + report.getDeathsVm());
            row.add("" + report.getIcu());
            row.add("" + report.getIcuVm());

            writer.writeNext(row.toArray(new String[]{}));

            row = new ArrayList<>();
        }

        writer.close();

        return String.join("\n", Files.readAllLines(path));
    }

    private String readFile(String filename) throws Exception {
        Path path = Paths.get(reportTgtDir).resolve(filename);
        return String.join("\n", Files.readAllLines(path));
    }

    private String writeFile(String filename, String[] header, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) throws Exception {
        Path path = Paths.get(reportTgtDir).resolve(filename);
        CSVWriter writer =
                new CSVWriter(Files.newBufferedWriter(path, StandardOpenOption.CREATE));

        writer.writeNext(header);

        writeLines(writer, reports, intFunction);

        writer.close();

        return String.join("\n", Files.readAllLines(path));
    }

    private void writeLines(CSVWriter writer, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) {
        List<String> row = new ArrayList<>();

        for (ArrayReport report : reports) {
            row.add(report.getId());
            row.addAll(Arrays.stream(intFunction.apply(report)).map(Object::toString).collect(Collectors.toList()));

            writer.writeNext(row.toArray(new String[]{}));

            row = new ArrayList<>();
        }
    }

    @GetMapping("/reports/v2/daily")
    public Collection<ArrayReport> getReports(@RequestParam(name = "startDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                              @RequestParam(name = "endDate", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        List<ArrayReport> reportList = reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate).stream().map(ArrayReport::new).collect(Collectors.toList());

        //File file = Paths.get(REPORT_TGT_DIR, "daily").toFile();
        //objectMapper.writeValue(file, reportList);

        return reportList;
    }

    @GetMapping("/reports/v2/latest")
    public ArrayReport getLatestReport() {
        TreeSet<Report> reports = new TreeSet<>(Comparator.comparing(Report::getId));
        reports.addAll(reportRepository.findAll());

        return new ArrayReport(reports.last());
    }
}

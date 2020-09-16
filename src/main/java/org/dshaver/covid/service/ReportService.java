package org.dshaver.covid.service;

import org.dshaver.covid.dao.*;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final int TARGET_HOUR = 19;

    private final RawDataRepositoryV2 rawDataRepositoryV2;
    private final RawDataFileRepository rawDataFileRepository;
    private final RawDataDownloaderDelegator rawDataDownloader;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;
    private final IntermediaryDataService intermediaryDataService;
    private final HistogramReportRepository histogramReportDao;
    private final CsvService csvService;
    private final String reportTgtDir;
    private final FileRegistry fileRegistry;

    @Inject
    public ReportService(RawDataFileRepository rawDataFileRepository,
                         ReportFactory reportFactory,
                         RawDataRepositoryV2 rawDataRepositoryV2,
                         RawDataDownloaderDelegator rawDataDownloader,
                         ReportRepository reportRepository,
                         IntermediaryDataService intermediaryDataService,
                         HistogramReportRepository histogramReportDao,
                         CsvService csvService,
                         @Value("${covid.dirs.reports.csv}") String reportTgtDir,
                         FileRegistry fileRegistry) {
        this.rawDataFileRepository = rawDataFileRepository;
        this.reportFactory = reportFactory;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.rawDataDownloader = rawDataDownloader;
        this.reportRepository = reportRepository;
        this.intermediaryDataService = intermediaryDataService;
        this.histogramReportDao = histogramReportDao;
        this.csvService = csvService;
        this.reportTgtDir = reportTgtDir;
        this.fileRegistry = fileRegistry;
    }

    /**
     * Main entrypoint to check for new reports.
     */
    @Scheduled(cron = "0 15 * * * *")
    public DownloadResponse checkForData() throws Exception {
        DownloadResponse response = new DownloadResponse();

        // Check disk
        Optional<String> diskLatestId = fileRegistry.getLatestId(RawDataV2.class);

        diskLatestId.ifPresent(response::setPreviousLatestId);

        // Download
        RawData data = rawDataDownloader.download(RawDataV2.class);

        String downloadLatestId = data.getId();

        diskLatestId.ifPresent(response::setPreviousLatestId);

        if (diskLatestId.isPresent() && downloadLatestId != null && !diskLatestId.get().equals(downloadLatestId)) {
            response.setNewLatestId(data.getId());
            response.setFoundNew(true);
            process(data);
        } else {
            logger.info("No new data found! Latest id: {}", diskLatestId.get());
        }

        return response;
    }

    public void processRange(LocalDate startDate, LocalDate endDate) throws Exception {
        rawDataRepositoryV2.streamPaths()
                .map(path -> {
                    try {
                        return rawDataRepositoryV2.readFile(path);
                    } catch (IOException e) {
                        logger.error("Error parsing raw data from disk! " + path, e);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .filter(rawDataV2 -> !rawDataV2.getReportDate().isAfter(endDate))
                .filter(rawDataV2 -> !rawDataV2.getReportDate().isBefore(startDate))
                .forEach(this::process);
    }

    public void appendCsvs(Report report) {
        String[] header = csvService.createHeader(report);

        for (Epicurve epicurve : report.getEpicurves().values()) {
            String county = epicurve.getCounty().toLowerCase();
            logger.info("Writing csvs for {} on {}.", county, report.getReportDate());

            try {
                csvService.appendFile(getCountyPath(reportTgtDir, "cases", county), county, header, report, ArrayReport::getCases);
                csvService.appendFile(getCountyPath(reportTgtDir, "caseDeltas", county), county, header, report, ArrayReport::getCaseDeltas);
                csvService.appendFile(getCountyPath(reportTgtDir, "movingAvgs", county), county, header, report, ArrayReport::getMovingAvgs);
                csvService.appendFile(getCountyPath(reportTgtDir, "deaths", county), county, header, report, ArrayReport::getDeaths);
                csvService.appendFile(getCountyPath(reportTgtDir, "deathDeltas", county), county, header, report, ArrayReport::getDeathDeltas);
            } catch (Exception e) {
                logger.error("Could not write csvs for county " + county);
            }
        }
    }

    public void updateAllHeaders(String dir, Report report, String[] header) {
        try {
            for (Epicurve epicurve : report.getEpicurves().values()) {
                String county = epicurve.getCounty().toLowerCase();
                csvService.updateHeader(getCountyPath(dir, "cases", county), header);
                csvService.updateHeader(getCountyPath(dir, "caseDeltas", county), header);
                csvService.updateHeader(getCountyPath(dir, "movingAvgs", county), header);
                csvService.updateHeader(getCountyPath(dir, "deaths", county), header);
                csvService.updateHeader(getCountyPath(dir, "deathDeltas", county), header);
            }
        } catch (Exception e) {
            logger.error("Could not update headers in target csvs coming from report file", e);
        }
    }

    public void rewriteCsvs() {

    }

    private Path getCountyPath(String dir, String type, String county) {
        Path path = Paths.get(dir).resolve(String.format("%s.csv", type));
        if (county != null) {
            String filteredCounty = county.replace(" ", "-");
            filteredCounty = filteredCounty.replace("/", "");
            filteredCounty = filteredCounty.replace("\\", "");
            filteredCounty = filteredCounty.replace("unknown-state", "");
            filteredCounty = filteredCounty.replace("non-ga-resident", "non-georgia-resident");

            path = Paths.get(dir, county).resolve(String.format("%s_%s.csv", type, filteredCounty));
        }

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            logger.error("Error creating county csv path!", e);
        }

        return path;
    }

    private void process(RawData data) {
        // Save intermediate filtered json
        intermediaryDataService.saveAll(data);

        // Find previous report. It's fine if it doesn't exist.
        Report prevReport = reportRepository.findByReportDate(data.getReportDate().minusDays(1)).orElse(null);

        // Create report
        Optional<Report> optionalReport = Optional.empty();
        try {
            Report report = reportFactory.createReport(data, prevReport);

            // Calculate moving average
            report = MovingAverageCalculator.calculate(report, 7);

            // Save
            optionalReport = Optional.of(reportRepository.save(report));
        } catch (IOException ioe) {
            logger.error("Error saving report " + data.getId(), ioe);
        } catch (Exception e) {
            logger.error("Error creating report " + data.getId(), e);
        }

        // Ensure index is up to date
        fileRegistry.checkAndSaveIndex();

        optionalReport.ifPresent(this::appendCsvs);
    }
}

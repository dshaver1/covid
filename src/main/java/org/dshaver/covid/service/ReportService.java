package org.dshaver.covid.service;

import org.dshaver.covid.dao.*;
import org.dshaver.covid.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
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
        Optional<String> diskLatestId = fileRegistry.getLatestId(BasicFile.class);

        diskLatestId.ifPresent(response::setPreviousLatestId);

        // Download
        RawData data = rawDataDownloader.download(RawDataV2.class);

        String downloadLatestId = data.getId();

        if (data.getId() != null) {
            response.setNewLatestId(data.getId());
        }

        if (diskLatestId.isPresent() && downloadLatestId != null && !diskLatestId.get().equals(downloadLatestId)) {
            response.setFoundNew(true);
        }

        process(data);

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

    private void process(RawData data) {
        // Save intermediate filtered json
        intermediaryDataService.saveAll(data);

        // Find previous report. It's fine if it doesn't exist.
        Report prevReport = reportRepository.findByReportDate(data.getReportDate().minusDays(1)).orElse(null);

        // Create report
        try {
            Report report = reportFactory.createReport(data, prevReport);

            // Populate VM
            report = VmCalculator.populateVm(report, prevReport);

            // Calculate moving average
            report = MovingAverageCalculator.calculate(report, 7);

            // Save
            reportRepository.save(report);
        } catch (IOException ioe) {
            logger.error("Error saving report " + data.getId(), ioe);
        } catch (Exception e) {
            logger.error("Error creating report " + data.getId(), e);
        }

        // Ensure index is up to date
        fileRegistry.checkAndSaveIndex();
    }

    private boolean is1800(String dateString) {
        int diff = 18 - LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME).getHour();

        return diff <= 0;
    }
}

package org.dshaver.covid.service;

import org.dshaver.covid.dao.*;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final int TARGET_HOUR = 19;

    private final RawDataRepositoryV0 rawDataRepositoryV0;
    private final RawDataRepositoryV1 rawDataRepositoryV1;
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
    private final TaskExecutor taskExecutor;

    @Inject
    public ReportService(RawDataRepositoryV0 rawDataRepositoryV0,
                         RawDataRepositoryV1 rawDataRepositoryV1,
                         RawDataFileRepository rawDataFileRepository,
                         ReportFactory reportFactory,
                         RawDataRepositoryV2 rawDataRepositoryV2,
                         RawDataDownloaderDelegator rawDataDownloader,
                         ReportRepository reportRepository,
                         IntermediaryDataService intermediaryDataService,
                         HistogramReportRepository histogramReportDao,
                         CsvService csvService,
                         @Value("${covid.dirs.reports.csv}") String reportTgtDir,
                         FileRegistry fileRegistry,
                         @Qualifier("singleTaskExecutor") TaskExecutor taskExecutor) {
        this.rawDataRepositoryV0 = rawDataRepositoryV0;
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
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
        this.taskExecutor = taskExecutor;
    }

    /**
     * Main entrypoint to check for new reports.
     */
    @Scheduled(cron = "0 55 * * * *")
    public void checkForData() {
        checkForData(false);
    }


    public DownloadResponse checkForData(boolean force) {
        DownloadResponse response = new DownloadResponse();

        fileRegistry.putIndex(RawDataV2.class, rawDataRepositoryV2.scanDirectory());

        // Check disk
        Optional<String> diskLatestId = fileRegistry.getLatestId(RawDataV2.class);

        diskLatestId.ifPresent(response::setPreviousLatestId);

        // Download
        RawData data = rawDataDownloader.download(RawDataV2.class);

        String downloadLatestId = data.getId();

        diskLatestId.ifPresent(response::setPreviousLatestId);

        if (force || (diskLatestId.isPresent() && downloadLatestId != null && !diskLatestId.get().equals(downloadLatestId))) {
            response.setNewLatestId(data.getId());
            response.setFoundNew(true);
            process(data, true);
        } else {
            logger.info("No new data found! Latest id: {}", diskLatestId.get());
        }

        return response;
    }

    public void processRange(LocalDate startDate, LocalDate endDate, boolean deleteFirst, boolean saveIntermediate) {
        if (deleteFirst) {
            csvService.deleteAllCsvs(reportTgtDir);
        }

        Stream.concat(rawDataRepositoryV0.streamSelectedPaths().map(rawDataRepositoryV0::readFile), Stream.concat(
                rawDataRepositoryV1.streamSelectedPaths().map(rawDataRepositoryV1::readFile),
                rawDataRepositoryV2.streamSelectedPaths().map(rawDataRepositoryV2::readFile)))
                .filter(Objects::nonNull)
                .filter(rawData -> !rawData.getReportDate().isAfter(endDate))
                .filter(rawData -> !rawData.getReportDate().isBefore(startDate))
                .forEach(data -> process(data, saveIntermediate));
    }

    public void appendCsvs(Report report) {
        String[] header = csvService.createHeader(report);

        logger.info("Writing csvs for all counties on {}.", report.getReportDate());

        for (Epicurve epicurve : report.getEpicurves().values()) {
            String county = epicurve.getCounty().toLowerCase();

            try {
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "cases", county), county, header, report, ArrayReport::getCases);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "caseDeltas", county), county, header, report, ArrayReport::getCaseDeltas);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "movingAvgs", county), county, header, report, ArrayReport::getMovingAvgs);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "deaths", county), county, header, report, ArrayReport::getDeaths);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "deathDeltas", county), county, header, report, ArrayReport::getDeathDeltas);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "pcrTests", county), county, header, report, ArrayReport::getPcrTest);
                csvService.appendFile(csvService.getCountyFilePath(reportTgtDir, "pcrPositives", county), county, header, report, ArrayReport::getPcrPos);
                csvService.appendSummary(csvService.getCountyFilePath(reportTgtDir, "summary", county), county, report);

            } catch (Exception e) {
                logger.error("Could not write csvs for county " + county);
            }
        }

        //logger.info("Updating all csv headers for report date {}.", report.getReportDate());
        //updateAllHeaders(report, header);
    }

/*    public void updateAllHeaders(Report report, String[] header) {
        try {
            for (Epicurve epicurve : report.getEpicurves().values()) {
                String county = epicurve.getCounty().toLowerCase();
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "cases", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "caseDeltas", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "movingAvgs", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "deaths", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "deathDeltas", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "pcrTests", county), header);
                csvService.updateHeader(csvService.getCountyFilePath(reportTgtDir, "pcrPositives", county), header);
            }
        } catch (Exception e) {
            logger.error("Could not update headers in target csvs coming from report file", e);
        }
    }*/
    private void process(RawData data) {
        process(data, true);
    }

    private void process(RawData data, boolean saveIntermediate) {
        logger.info("Entering process({},{})", data.getId(), saveIntermediate);
        if (saveIntermediate) {
            // Save intermediate filtered json if requested
            intermediaryDataService.saveAll(data);
        }

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

        optionalReport.ifPresent($ -> taskExecutor.execute(() -> appendCsvs($)));
    }
}

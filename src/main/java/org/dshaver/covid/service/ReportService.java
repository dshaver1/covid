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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    private final RawDataRepositoryV3 rawDataRepositoryV3;
    private final RawDataDownloaderDelegator rawDataDownloader;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;
    private final IntermediaryDataService intermediaryDataService;
    private final HistogramReportRepository histogramReportDao;
    private final HistogramReportFactory histogramReportFactory;
    private final CsvService csvService;
    private final String reportTgtDir;
    private final FileRegistry fileRegistry;
    private final TaskExecutor taskExecutor;
    private final AggregateReportFactory aggregateReportFactory;

    @Inject
    public ReportService(RawDataRepositoryV0 rawDataRepositoryV0,
                         RawDataRepositoryV1 rawDataRepositoryV1,
                         ReportFactory reportFactory,
                         RawDataRepositoryV2 rawDataRepositoryV2,
                         RawDataRepositoryV3 rawDataRepositoryV3,
                         RawDataDownloaderDelegator rawDataDownloader,
                         ReportRepository reportRepository,
                         IntermediaryDataService intermediaryDataService,
                         HistogramReportRepository histogramReportDao,
                         HistogramReportFactory histogramReportFactory,
                         CsvService csvService,
                         @Value("${covid.dirs.reports.csv}") String reportTgtDir,
                         FileRegistry fileRegistry,
                         @Qualifier("singleTaskExecutor") TaskExecutor taskExecutor,
                         AggregateReportFactory aggregateReportFactory) {
        this.rawDataRepositoryV0 = rawDataRepositoryV0;
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.reportFactory = reportFactory;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.rawDataRepositoryV3 = rawDataRepositoryV3;
        this.rawDataDownloader = rawDataDownloader;
        this.reportRepository = reportRepository;
        this.intermediaryDataService = intermediaryDataService;
        this.histogramReportDao = histogramReportDao;
        this.histogramReportFactory = histogramReportFactory;
        this.csvService = csvService;
        this.reportTgtDir = reportTgtDir;
        this.fileRegistry = fileRegistry;
        this.taskExecutor = taskExecutor;
        this.aggregateReportFactory = aggregateReportFactory;
    }

    /**
     * Main entrypoint to check for new reports.
     */
    @Scheduled(cron = "15,21,3,9 55 * * * *")
    public void checkForData() {
        checkForData(false);
    }


    public DownloadResponse checkForData(boolean force) {
        DownloadResponse response = new DownloadResponse();

        fileRegistry.putIndex(RawDataV3.class, rawDataRepositoryV3.scanDirectory());

        // Check disk
        Optional<String> diskLatestId = fileRegistry.getLatestId(RawDataV3.class);

        diskLatestId.ifPresent(response::setPreviousLatestId);

        // Download
        RawData data = rawDataDownloader.download(RawDataV3.class);

        String downloadLatestId = data.getId();

        diskLatestId.ifPresent(response::setPreviousLatestId);

        if (force || (diskLatestId.isPresent() && downloadLatestId != null && !diskLatestId.get().equals(downloadLatestId))) {
            response.setNewLatestId(data.getId());
            response.setFoundNew(true);
            process(data, true);
            processHistogram(data.getReportDate());
            aggregateReportFactory.createAggregateReport(data.getReportDate());

            // Ensure index is up to date
            fileRegistry.checkAndSaveIndex();
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
                Stream.concat(rawDataRepositoryV2.streamSelectedPaths().map(rawDataRepositoryV2::readFile),
                        rawDataRepositoryV3.streamSelectedPaths().map(rawDataRepositoryV3::readFile))))
                .filter(Objects::nonNull)
                .filter(rawData -> !rawData.getReportDate().isAfter(endDate))
                .filter(rawData -> !rawData.getReportDate().isBefore(startDate))
                .forEach(data -> process(data, saveIntermediate));

        // Ensure index is up to date
        fileRegistry.checkAndSaveIndex();

        processHistogramRange(startDate, endDate);
    }

    public void processHistogram(LocalDate endDate) {
        HistogramReportContainer histogramReportContainer = histogramReportFactory.createHistogramReport(endDate);
        appendHistogramCsvs(histogramReportContainer);
    }

    public void processHistogramRange(LocalDate startDate, LocalDate endDate) {
        histogramReportFactory.createAllHistogramReports(startDate, endDate);
        createHistogramCsvs(startDate, endDate);
    }

    public void processHistogramRange(LocalDate startDate, LocalDate endDate, Integer windowSize) {
        histogramReportFactory.createAllHistogramReports(startDate, endDate, windowSize);
        createHistogramCsvs(startDate, endDate);
    }

    public void createHistogramCsvs(LocalDate startDate, LocalDate endDate) {
        histogramReportDao.findByReportDateBetweenOrderByIdAsc(startDate, endDate)
                .sorted(Comparator.comparing(HistogramReportContainer::getReportDate))
                .forEachOrdered(this::appendHistogramCsvs);
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
                logger.error("Could not write csvs for county " + county, e);
            }
        }
    }

    public void appendHistogramCsvs(HistogramReportContainer histogramReport) {
        List<String> histogramHeaderList = new ArrayList<>();
        histogramHeaderList.add("reportDate");
        histogramHeaderList.addAll(IntStream.range(0, 100).mapToObj(Integer::toString).collect(Collectors.toList()));
        String[] histogramHeader = histogramHeaderList.toArray(new String[]{});

        for (HistogramReportV2 currentReport : histogramReport.getCountyHistogramMap().values()) {
            String county = currentReport.getCounty().toLowerCase();

            csvService.appendHistogramFile(csvService.getCountyFilePath(reportTgtDir, "histogramCases", county), county, histogramHeader, histogramReport, HistogramReportV2::getCasesPercentageHist);
            csvService.appendHistogramFile(csvService.getCountyFilePath(reportTgtDir, "histogramCasesCum", county), county, histogramHeader, histogramReport, HistogramReportV2::getCasesPercentageCumulative);
        }
    }

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

        optionalReport.ifPresent($ -> taskExecutor.execute(() -> appendCsvs($)));
    }
}

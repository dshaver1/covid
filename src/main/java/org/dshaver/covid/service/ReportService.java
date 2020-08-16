package org.dshaver.covid.service;

import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.RawDataRepositoryDelegator;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.dshaver.covid.service.extractor.EpicurveExtractorImpl2.COUNTY_FILTER;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final int TARGET_HOUR = 19;

    private final RawDataFileRepository rawDataFileRepository;
    private final RawDataRepositoryDelegator rawDataRepository;
    private final RawDataDownloaderDelegator rawDataDownloader;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;
    private final HistogramReportRepository histogramReportRepository;
    private final CsvService csvService;
    private final String reportTgtDir;

    @Inject
    public ReportService(RawDataFileRepository rawDataFileRepository,
                         ReportFactory reportFactory,
                         RawDataRepositoryDelegator rawDataRepository,
                         RawDataDownloaderDelegator rawDataDownloader,
                         ReportRepository reportRepository,
                         HistogramReportRepository histogramReportRepository,
                         CsvService csvService,
                         @Value("${covid.report.target.v2.dir}") String reportTgtDir) {
        this.rawDataFileRepository = rawDataFileRepository;
        this.reportFactory = reportFactory;
        this.rawDataRepository = rawDataRepository;
        this.rawDataDownloader = rawDataDownloader;
        this.reportRepository = reportRepository;
        this.histogramReportRepository = histogramReportRepository;
        this.csvService = csvService;
        this.reportTgtDir = reportTgtDir;
    }

    /**
     * Main entrypoint to check for new reports.
     */
    @Scheduled(cron = "0 15 * * * *")
    public DownloadResponse checkForData() {
        DownloadResponse response = new DownloadResponse();
        if (checkAndAppend(reportTgtDir)) {
            // Download
            RawData data = rawDataDownloader.download(RawDataV2.class);

            response = saveReportFromRawData(data);

            generateAllCsvs(reportTgtDir);
        } else {
            response.setFoundNew(false);
        }

        return response;
    }

    public String[] createHeader(List<File> files) {
        List<String> headerList = new ArrayList<>();
        headerList.add("id");
        String[] header = new String[0];
        try {
            headerList.addAll(Arrays.stream(new ArrayReport(reportFactory.createReport(rawDataDownloader.transform(files.get(files.size() - 1)), null)).getCurveDates())
                    .map(LocalDate::toString)
                    .collect(Collectors.toList()));

            header = headerList.toArray(new String[]{});

        } catch (Exception e) {
            logger.error("Could not extract header from {}!", files.get(files.size() - 1));
        }

        return header;
    }

    public String[] createHeader(File file) {
        List<String> headerList = new ArrayList<>();
        headerList.add("id");
        String[] header = new String[0];
        try {
            ArrayReport arrayReport = new ArrayReport(reportFactory.createReport(rawDataDownloader.transform(file), null));

            header = Arrays.stream(arrayReport.getCurveDates()).map(LocalDate::toString).collect(Collectors.toList()).toArray(new String[]{});
        } catch (Exception e) {
            logger.error("Could not extract header from {}!", file.getName());
        }

        return header;
    }

    /**
     * Check for new data and rewrite to csvs if needed.
     */
    public boolean checkAndRewriteCsvs(String reportTgtDir, boolean force) {
        logger.info("Saving CSVs with no persistence to database!");
        boolean success = true;

        File latestFile = rawDataFileRepository.getLatestRawDataFile();

        RawData downloaded = rawDataDownloader.download(RawDataV2.class);
        HistogramReport histogramReport = histogramReportRepository.findAllByOrderByIdDesc().get(0);

        if (!latestFile.getName().contains(downloaded.getId().replace(":", "")) || force) {
            logger.info("New data found! Proceeding to update csvs.");

            COUNTY_FILTER.forEach(d -> csvService.delete(reportTgtDir, d));

            List<File> files = rawDataFileRepository.getAllRawDataFiles();

            String[] header = createHeader(files);

            Report previousReport = null;
            try {
                RawData currentRawData = rawDataDownloader.transform(files.get(0));
                previousReport = reportFactory.createReport(currentRawData, null);
            } catch (Exception e) {
                logger.error("Error processing previous day's report! " + files.get(0).getName(), e);
            }

            for (File currentFile : files) {
                previousReport = write(currentFile, previousReport, header, histogramReport);
            }
        } else {
            logger.info("No new data. Existing file {} has the same id as new file {}!", latestFile.getName(), downloaded.getId());
        }

        return success;
    }

    public boolean checkAndAppend(String reportTgtDir) {
        logger.info("Saving CSVs with no persistence to database!");
        boolean foundNew = false;

        File latestFile = rawDataFileRepository.getLatestRawDataFile();

        RawData downloaded = rawDataDownloader.download(RawDataV2.class);
        HistogramReport histogramReport = histogramReportRepository.findAllByOrderByIdDesc().get(0);

        if (!latestFile.getName().contains(downloaded.getId().replace(":", ""))) {
            foundNew = true;
            logger.info("New data found! Proceeding to update csvs.");

            List<File> files = rawDataFileRepository.getAllRawDataFiles();

            String[] header = createHeader(files);

            updateAllHeaders(reportTgtDir, files.get(files.size() - 1), header);

            Report previousReport = null;
            try {
                RawData currentRawData = rawDataDownloader.transform(files.get(files.size() - 2));
                previousReport = reportFactory.createReport(currentRawData, null);
            } catch (Exception e) {
                logger.error("Error processing previous day's report! " + files.get(files.size() - 2).getName(), e);
            }

            write(files.get(files.size() - 1), previousReport, header, histogramReport);
        } else {
            logger.info("No new data. Existing file {} has the same id as new file {}!", latestFile.getName(), downloaded.getId());
        }

        return foundNew;
    }

    public void updateAllHeaders(String dir, File file, String[] header) {
        try {
            RawData currentRawData = rawDataDownloader.transform(file);
            Report report = reportFactory.createReport(currentRawData, null);
            for (Epicurve epicurve : report.getEpicurves().values()) {
                csvService.updateHeader(dir, String.format("cases_%s.csv", epicurve.getCounty().toLowerCase()), header);
                csvService.updateHeader(dir, String.format("caseDeltas_%s.csv", epicurve.getCounty().toLowerCase()), header);
                //csvService.updateHeader(dir, String.format("caseProjections_%s.csv", epicurve.getCounty().toLowerCase()), header);
                csvService.updateHeader(dir, String.format("movingAvgs_%s.csv", epicurve.getCounty().toLowerCase()), header);
                csvService.updateHeader(dir, String.format("deaths_%s.csv", epicurve.getCounty().toLowerCase()), header);
                csvService.updateHeader(dir, String.format("deathDeltas_%s.csv", epicurve.getCounty().toLowerCase()), header);
            }
        } catch (Exception e) {
            logger.error("Could not update headers in target csvs coming from report file " + file.getName(), e);
        }
    }

    public Report write(File currentFile, Report previousReport, String[] header, HistogramReport histogramReport) {
        if (histogramReport == null) {
            histogramReport = histogramReportRepository.findAllByOrderByIdDesc().get(0);
        }

        if (header == null) {
            header = createHeader(currentFile);
        }

        Report currentReport = null;
        RawData currentRawData = null;

        try {
            logger.info("Creating report from file {}.", currentFile.getName());
            currentRawData = rawDataDownloader.transform(currentFile);
            currentReport = reportFactory.createReport(currentRawData, previousReport);

            // Populate VM
            currentReport = VmCalculator.populateVm(currentReport, previousReport);

            // Extrapolate
            currentReport = EpicurveExtrapolator.extrapolateCases(currentReport, histogramReport);

            // Calculate moving average
            currentReport = MovingAverageCalculator.calculate(currentReport, 7);

            for (Epicurve epicurve : currentReport.getEpicurves().values()) {
                logger.info("Writing csvs for {} on {}.", epicurve.getCounty().toLowerCase(), currentRawData.getReportDate());
                csvService.appendFile(reportTgtDir, "cases", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getCases);
                csvService.appendFile(reportTgtDir, "caseDeltas", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getCaseDeltas);
                //csvService.appendFile(reportTgtDir, "caseProjections", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getCaseProjections);
                csvService.appendFile(reportTgtDir, "movingAvgs", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getMovingAvgs);
                csvService.appendFile(reportTgtDir, "deaths", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getDeaths);
                csvService.appendFile(reportTgtDir, "deathDeltas", epicurve.getCounty().toLowerCase(), header, currentReport, ArrayReport::getDeathDeltas);
                //csvService.writeSummary(reportTgtDir, "summary.csv", filteredReports);
            }
        } catch (Exception e) {
            logger.error("Could not transform file " + currentFile.getName(), e);
        }

        return currentReport;
    }

    public boolean generateAllCsvs(String reportTgtDir) {
        logger.info("Saving CSVs!");
        boolean success = true;
        LocalDate defaultedStartDate = LocalDate.of(2020, 1, 1);
        LocalDate defaultedEndDate = LocalDate.of(2030, 1, 1);

        Map<LocalDate, List<ArrayReport>> groupedReports =
                reportRepository.findByReportDateBetweenOrderByIdAsc(defaultedStartDate, defaultedEndDate).stream()
                        .map(ArrayReport::new)
                        .collect(Collectors.groupingBy(ArrayReport::getReportDate));

        TreeSet<ArrayReport> filteredReports = new TreeSet<>(Comparator.comparing(ArrayReport::getId));
        groupedReports.forEach((reportDate, reports) -> {
            ArrayReport selectedReport = reports.get(0);

            for (ArrayReport currentReport : reports) {
                int selectedDiff = Math.abs(LocalDateTime.parse(selectedReport.getId()).getHour() - TARGET_HOUR);
                int currentDiff = Math.abs(LocalDateTime.parse(currentReport.getId()).getHour() - TARGET_HOUR);

                selectedReport = currentDiff < selectedDiff ? currentReport : selectedReport;

                if (currentDiff == 0) {
                    break;
                }
            }

            filteredReports.add(selectedReport);
        });

        String[] headerArray = csvService.createHeader(filteredReports);
        List<String> distributionList = new ArrayList<>();
        distributionList.add("id");
        distributionList.addAll(IntStream.rangeClosed(-99, 0).boxed().map(i -> -i + -99).map(Object::toString).collect(Collectors.toList()));
        String[] distributionHeader = distributionList.toArray(new String[0]);

        try {
            csvService.writeFile(reportTgtDir, "cases.csv", headerArray, filteredReports, ArrayReport::getCases);
            csvService.writeFile(reportTgtDir, "caseDeltas.csv", headerArray, filteredReports, ArrayReport::getCaseDeltas);
            //csvService.writeFile(reportTgtDir, "caseProjections.csv", headerArray, filteredReports, ArrayReport::getCaseProjections);
            csvService.writeFile(reportTgtDir, "movingAvgs.csv", headerArray, filteredReports, ArrayReport::getMovingAvgs);
            csvService.writeFile(reportTgtDir, "deaths.csv", headerArray, filteredReports, ArrayReport::getDeaths);
            csvService.writeFile(reportTgtDir, "deathDeltas.csv", headerArray, filteredReports, ArrayReport::getDeathDeltas);
            csvService.writeFile(reportTgtDir, "caseDeltasNormalized.csv", distributionHeader, filteredReports, ArrayReport::getCaseDeltasNormalized);
            csvService.writeFile(reportTgtDir, "deathDeltasNormalized.csv", distributionHeader, filteredReports, ArrayReport::getDeathDeltasNormalized);
            csvService.writeSummary(reportTgtDir, "summary.csv", filteredReports);
        } catch (Exception e) {
            logger.error("Could not save csvs!", e);

            success = false;
        }

        return success;
    }

    public void bulkProcess(LocalDate startDate, LocalDate endDate, boolean deleteFirst) {
        List<RawData> allData = rawDataRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate, RawDataV1.class);
        allData.addAll(rawDataRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate, ManualRawData.class));
        allData.addAll(rawDataFileRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate));

        if (deleteFirst) {
            logger.info("Deleting all reports!");
            reportRepository.deleteAll();
            rawDataRepository.deleteAll(RawDataV2.class);
        }

        //allData.stream().filter(data -> data.getClass().equals(RawDataV2.class)).forEach(rawDataRepository::save);

        List<Report> candidatePreviousReports = reportRepository.findByReportDateOrderByIdAsc(allData.get(0).getReportDate().minusDays(1));

        Report prevReport = candidatePreviousReports.size() > 0 ? candidatePreviousReports.stream().skip(candidatePreviousReports.size() - 1).findFirst().get() : null;

        HistogramReport histogramReport = histogramReportRepository.findAllByOrderByIdDesc().get(0);

        Map<LocalDate, List<RawData>> groupedByReportDate = allData.stream().collect(Collectors.groupingBy(RawData::getReportDate));

        for (LocalDate currentDate : groupedByReportDate.keySet().stream().sorted().collect(Collectors.toList())) {
            List<RawData> currentDataList = groupedByReportDate.get(currentDate);
            for (int idx = 0; idx < currentDataList.size(); idx++) {
                RawData currentData = currentDataList.get(idx);
                boolean lastElement = idx + 1 >= currentDataList.size();

                // Only save if it's the 6pm update or if there's no more reports after this for the day.
                if (is1800(currentData.getId()) || lastElement) {
                    try {
                        // Create report
                        Report report = reportFactory.createReport(currentData, prevReport);

                        // Populate VM
                        report = VmCalculator.populateVm(report, prevReport);

                        // Extrapolate
                        report = EpicurveExtrapolator.extrapolateCases(report, histogramReport);

                        // Calculate moving average
                        report = MovingAverageCalculator.calculate(report, 7);

                        // Save
                        reportRepository.save(report);
                        prevReport = report;
                        break;
                    } catch (DuplicateKeyException e) {
                        logger.info("Already saved this report. Skipping...");
                    } catch (Exception e) {
                        logger.error("Could not create report! " + currentData, e);
                    }
                }
            }
        }

        try {
            histogramReportRepository.save(new HistogramReport(reportRepository.findAllByOrderByIdAsc()));
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this histogram report. Skipping... ");
        }
    }

    public DownloadResponse saveReportFromRawData(RawData data) {
        DownloadResponse response = new DownloadResponse();
        // Save raw data
        try {
            //rawDataRepository.save(data);
            //logger.info("Done saving RawDataV2.");
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this page. Skipping...");
        }

        try {
            // Get previous report
            List<Report> lastFewReports = reportRepository.findByReportDateBetweenOrderByIdAsc(data.getReportDate().minusDays(2), data.getReportDate());
            Report prevReport = lastFewReports.get(lastFewReports.size() - 1);

            // If previous report is on the same date, but the downloaded report is newer, delete the previous report.
            if (prevReport.getReportDate().equals(data.getReportDate()) && !prevReport.getId().equals(data.getId()) && !is1800(prevReport.getId())) {
                reportRepository.delete(prevReport);
                prevReport = lastFewReports.get(lastFewReports.size() - 2);
            }

            if (prevReport.getId().equals(data.getId())) {
                prevReport = lastFewReports.get(lastFewReports.size() - 2);
            } else {
                response.setFoundNew(true);
            }

            // Populate VM data
            Report report = VmCalculator.populateVm(reportFactory.createReport(data, prevReport), prevReport);

            // Save histogram report object
/*            HistogramReport histogramReport = new HistogramReport(reportRepository.findAllByOrderByIdAsc());
            try {
                logger.info("Saving histogram report for {}.", report.getId());
                histogramReportRepository.save(histogramReport);
            } catch (DuplicateKeyException e) {
                logger.info("Already saved this histogram report. Skipping... ");
            }*/

            // Extrapolate
            //report = EpicurveExtrapolator.extrapolateCases(report, histogramReport);

            // Calculate moving average
            report = MovingAverageCalculator.calculate(report, 7);

            response.setReport(report);
            logger.info("Done creating Report.");

            // Save report object
            reportRepository.save(report);

            logger.info("Done saving report");
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this report. Skipping... ");
        } catch (Exception e) {
            logger.info("Could not create report from rawData! " + data, e);
        }

        return response;
    }

    private boolean is1800(String dateString) {
        int diff = 18 - LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME).getHour();

        return diff <= 0;
    }
}

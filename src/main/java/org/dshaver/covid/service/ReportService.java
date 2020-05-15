package org.dshaver.covid.service;

import org.dshaver.covid.dao.RawDataRepositoryDelegator;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV2;
import org.dshaver.covid.domain.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final RawDataRepositoryDelegator rawDataRepository;
    private final RawDataDownloaderDelegator rawDataDownloader;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;

    @Inject
    public ReportService(ReportFactory reportFactory,
                         RawDataRepositoryDelegator rawDataRepository,
                         RawDataDownloaderDelegator rawDataDownloader,
                         ReportRepository reportRepository) {
        this.reportFactory = reportFactory;
        this.rawDataRepository = rawDataRepository;
        this.rawDataDownloader = rawDataDownloader;
        this.reportRepository = reportRepository;
    }

    /**
     * Main entrypoint to check for new reports.
     */
    @Scheduled(cron = "0 0 * * * *")
    public DownloadResponse checkForData() {
        // Download
        RawData data = rawDataDownloader.download(RawDataV2.class);

        return saveReportFromRawData(data);
    }

    public void bulkProcess(LocalDate startDate, LocalDate endDate, boolean deleteFirst, Class<? extends RawData> rawDataClass) {
        List<RawData> allData = rawDataRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate, rawDataClass);

        if (deleteFirst) {
            logger.info("Deleting all reports!");
            reportRepository.deleteAll();
        }

        Report prevReport = null;
        RawData mostRecentData = allData.get(allData.size() - 1);

        for (RawData rawData : allData) {
            // Always save the first report. After that, only save if the day is after the last day, and the hour is equal to or after 1800.
            if (prevReport == null || rawData.equals(mostRecentData) || (rawData.getReportDate().isAfter(prevReport.getReportDate()) && is1800(rawData.getId()))) {
                try {
                    Report report = VmCalculator.populateVm(reportFactory.createReport(rawData, prevReport), prevReport);
                    reportRepository.save(report);
                    prevReport = report;
                } catch (DuplicateKeyException e) {
                    logger.info("Already saved this report. Skipping...");
                } catch (Exception e) {
                    logger.error("Could not create report! " + rawData, e);
                }
            }
        }
    }

    public DownloadResponse saveReportFromRawData(RawData data) {
        DownloadResponse response = new DownloadResponse();
        // Save raw data
        try {
            rawDataRepository.save(data);
            logger.info("Done saving RawDataV2.");
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this page. Skipping...");
        }

        try {
            // Get previous report
            List<Report> allExistingReports = reportRepository.findAllByOrderByIdAsc();
            Report prevReport = allExistingReports.get(allExistingReports.size() - 1);

            // If previous report is on the same date, but the downloaded report is newer, delete the previous report.
            if (prevReport.getReportDate().equals(data.getReportDate()) && !prevReport.getId().equals(data.getId()) && !is1800(prevReport.getId())) {
                reportRepository.delete(prevReport);
                prevReport = allExistingReports.get(allExistingReports.size() - 2);
            }

            // Populate VM data
            Report report = VmCalculator.populateVm(reportFactory.createReport(data, prevReport), prevReport);
            response.setReport(report);
            logger.info("Done creating Report.");

            // Save report object
            reportRepository.insert(report);

            logger.info("Done saving report");

            response.setFoundNew(true);
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

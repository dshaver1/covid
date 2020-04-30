package org.dshaver.covid.service;

import org.dshaver.covid.dao.RawDataRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Service
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final RawDataRepository rawDataRepository;
    private final RawDataDownloader rawDataDownloader;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;
    private final String downloadUrl;

    @Inject
    public ReportService(ReportFactory reportFactory,
                         RawDataRepository rawDataRepository,
                         RawDataDownloader rawDataDownloader,
                         ReportRepository reportRepository,
                         @Value("${covid.download.url}") String downloadUrl) {
        this.reportFactory = reportFactory;
        this.rawDataRepository = rawDataRepository;
        this.rawDataDownloader = rawDataDownloader;
        this.reportRepository = reportRepository;
        this.downloadUrl = downloadUrl;
    }

    /**
     * Wrote this to do the one-time download from internet archives.
     */
    //@PostConstruct
    public void bulkDownload() {
        List<String> urls = new ArrayList<>();
        //6PM reports
        urls.add("https://web.archive.org/web/20200401233102/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200402233057/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200403233059/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200404233057/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200406235830/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200407233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200408233107/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200409233108/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200410233107/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200411233109/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200412233116/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200413233106/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200414233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200415233104/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200416233056/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200417233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200418233059/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200420233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200421233835/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200422233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200423233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200424233102/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200425233100/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200426233107/https://d20s4vd27d0hk0.cloudfront.net/");

        //12PM reports
        urls.add("https://web.archive.org/web/20200328113218/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200331185130/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200401155500/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200402130332/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200403161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200404161034/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200405181443/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200406161114/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200407161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200408161036/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200409162105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200410161045/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200411161053/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200412162041/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200413161051/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200414161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200415161043/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200416161055/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200417161039/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200418161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200419161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200420161039/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200421161041/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200422161044/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200423161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200424161044/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200425161043/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200426162519/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200427161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200428170749/https://d20s4vd27d0hk0.cloudfront.net/");

        urls.forEach(this::downloadData);
    }

    /**
     * Wrote this to do the one-time download from internet archives.
     */
    public void bulkProcess(boolean deleteFirst) {
        List<RawData> allData = rawDataRepository.findAll();

        if (deleteFirst) {
            logger.info("Deleting all reports!");
            reportRepository.deleteAll();
        }

        for (RawData rawData : allData) {
            try {
                Report report = reportFactory.createReport(rawData);
                reportRepository.insert(report);
            } catch (DuplicateKeyException e) {
                logger.info("Already saved this report. Skipping...");
            } catch (Exception e) {
                logger.error("Could not create report! " + rawData, e);
            }
        }
    }

    /**
     * Main entrypoint to check for new reports.
     */
    //@PostConstruct
    @Scheduled(cron = "0 0 * * * *")
    public DownloadResponse checkForData() {
        return downloadData(downloadUrl);
    }


    /**
     * Downloads latest report. If we've already seen it, do nothing.
     *
     * @param url Configured in covid.download.url.
     */
    public DownloadResponse downloadData(String url) {
        DownloadResponse response = new DownloadResponse();
        RawData rawData = rawDataDownloader.download(url);

        logger.info("Got RawData from {}.", url);

        try {
            rawDataRepository.insert(rawData);
            logger.info("Done saving RawData.");
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this page. Skipping...");
        }

        try {
            Report report = reportFactory.createReport(rawData);
            response.setReport(report);
            logger.info("Done creating Report.");

            reportRepository.insert(report);

            logger.info("Done saving report");

            response.setFoundNew(true);
        } catch (DuplicateKeyException e) {
            logger.info("Already saved this report. Skipping...");
        } catch (Exception e) {
            logger.info("Could not create report from rawData! {}", rawData);
        }

        /*
        if (reportList.size() > 1) {
            logger.info("Comparing to previous run...");
            Report lastReport = reportList.get(reportList.size()-2);
            logger.info(
                    "\n\t--------------------Total Deaths--------------------- \n" +
                            "\t {} \t {}\n" +
                            "\t {} \t\t\t\t\t\t {}",
                    report.getReportTime(), lastReport.getReportTime(),
                    report.getTotalDeaths(), lastReport.getTotalDeaths());
            logger.info(
                    "\n\t--------------------Total Cases--------------------- \n" +
                            "\t {} \t {}\n" +
                            "\t {} \t\t\t\t\t\t {}",
                    report.getReportTime(), lastReport.getReportTime(),
                    report.getTotalCases(), lastReport.getTotalCases());

            if (report.getTotalCases() != lastReport.getTotalCases() || report.getTotalDeaths() != lastReport.getTotalDeaths()) {
                report.writeReport();
            }

            if (report.getTotalCases() != lastReport.getTotalCases()) {
                logger.info("!!!CASE DATA UPDATED!!!");
            }

            if (report.getTotalDeaths() != lastReport.getTotalDeaths()) {
                logger.info("!!!DEATH DATA UPDATED!!!");
            }
        }
        */

        return response;
    }
}

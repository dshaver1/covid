package org.dshaver.covid.service;

import org.dshaver.covid.dao.RawDataRepository;
import org.dshaver.covid.dao.RawDataRepository2;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.RawDataV2;
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
    private final RawDataRepository2 rawDataRepository2;
    private final RawDataDownloader rawDataDownloader;
    private final RawDataDownloader2 rawDataDownloader2;
    private final ReportFactory reportFactory;
    private final ReportRepository reportRepository;
    private final String downloadUrl1;
    private final String downloadUrl2;

    @Inject
    public ReportService(ReportFactory reportFactory,
                         RawDataRepository rawDataRepository,
                         RawDataRepository2 rawDataRepository2,
                         RawDataDownloader rawDataDownloader,
                         RawDataDownloader2 rawDataDownloader2,
                         ReportRepository reportRepository,
                         @Value("${covid.download.url}") String downloadUrl1,
                         @Value("${covid.download.url2}") String downloadUrl2) {
        this.reportFactory = reportFactory;
        this.rawDataRepository = rawDataRepository;
        this.rawDataRepository2 = rawDataRepository2;
        this.rawDataDownloader = rawDataDownloader;
        this.rawDataDownloader2 = rawDataDownloader2;
        this.reportRepository = reportRepository;
        this.downloadUrl1 = downloadUrl1;
        this.downloadUrl2 = downloadUrl2;
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

        urls.forEach(this::downloadDataV1);
    }

    /**
     * Wrote this to do the one-time download from internet archives.
     */
    public void bulkProcessV1Data(boolean deleteFirst) {
        List<RawDataV1> allData = rawDataRepository.findAll();

        if (deleteFirst) {
            logger.info("Deleting all reports!");
            reportRepository.deleteAll();
        }

        for (RawDataV1 rawData : allData) {
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

    public void bulkProcessV2Data(boolean deleteFirst) {
        List<RawDataV2> allData = rawDataRepository2.findAll();

        if (deleteFirst) {
            logger.info("Deleting all reports!");
            reportRepository.deleteAll();
        }

        for (RawDataV2 rawData : allData) {
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
    public List<DownloadResponse> checkForData() {
        List<DownloadResponse> responses = new ArrayList<>();
        //responses.add(downloadDataV1(downloadUrl1));
        responses.add(downloadDataV2(downloadUrl2));

        return responses;
    }

    /**
     * Downloads latest report in newfangled javascript format.
     *
     * @param url Configured in covid.download.url2.
     */
    public DownloadResponse downloadDataV2(String url) {
        DownloadResponse response = new DownloadResponse();
        RawDataV2 rawData = rawDataDownloader2.download(url);

        logger.info("Got RawDataV2 from {}.", url);

        try {
            rawDataRepository2.insert(rawData);
            logger.info("Done saving RawDataV2.");
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
            logger.info("Already saved this report. Skipping... ");
        } catch (Exception e) {
            logger.info("Could not create report from rawData! " + rawData, e);
        }

        return response;
    }


    /**
     * Downloads latest report. If we've already seen it, do nothing. Made for the original format
     *
     * @param url Configured in covid.download.url.
     */
    public DownloadResponse downloadDataV1(String url) {
        DownloadResponse response = new DownloadResponse();
        RawDataV1 rawData = rawDataDownloader.download(url);

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
            logger.info("Could not create report from rawData! " + rawData, e);
        }

        return response;
    }
}

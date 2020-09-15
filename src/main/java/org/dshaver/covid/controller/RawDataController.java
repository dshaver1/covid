package org.dshaver.covid.controller;

import org.dshaver.covid.domain.DownloadResponse;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.service.RawDataFileRepository;
import org.dshaver.covid.service.RawDataWriter;
import org.dshaver.covid.service.ReportService;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl1;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.time.LocalDate;

/**
 * Created by xpdf64 on 2020-06-12.
 */
@RestController
public class RawDataController {
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataWriter rawDataWriter;
    private final RawDataFileRepository fileRepository;
    private final EpicurveExtractorImpl1 extractorImpl1;
    private final EpicurveExtractorImpl2 extractorImpl2;
    private final ReportService reportService;

    @Inject
    public RawDataController(RawDataWriter rawDataWriter,
                             RawDataFileRepository fileRepository,
                             EpicurveExtractorImpl1 extractorImpl1,
                             EpicurveExtractorImpl2 extractorImpl2,
                             ReportService reportService) {
        this.rawDataWriter = rawDataWriter;
        this.fileRepository = fileRepository;
        this.extractorImpl1 = extractorImpl1;
        this.extractorImpl2 = extractorImpl2;
        this.reportService = reportService;
    }

    @PostMapping("/covid/api/poll")
    public DownloadResponse checkForData() throws Exception {
        return reportService.checkForData();
    }

    @PostMapping("/covid/api/reprocess")
    public void reprocess(@RequestParam(name = "startDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(name = "endDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? LocalDate.of(2030, 1, 1) : endDate.plusDays(1);

        reportService.processRange(defaultedStartDate, defaultedEndDate);
    }

    private String getExtension(RawData data) {
        if (data instanceof RawDataV1) {
            return ".html";
        } else {
            return ".js";
        }
    }
}

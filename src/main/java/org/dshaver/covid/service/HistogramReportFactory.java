package org.dshaver.covid.service;

import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.HistogramReport;
import org.dshaver.covid.domain.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistogramReportFactory {
    private static final Logger logger = LoggerFactory.getLogger(HistogramReportFactory.class);
    public final HistogramReportRepository histogramReportRepository;
    public final ReportRepository reportRepository;

    @Inject
    public HistogramReportFactory(HistogramReportRepository histogramReportRepository,
                                  ReportRepository reportRepository) {
        this.histogramReportRepository = histogramReportRepository;
        this.reportRepository = reportRepository;
    }

    public void createAllHistogramReports(LocalDate startDate, LocalDate endDate, Integer windowLength) {
        LocalDate currentDate = startDate.plusDays(windowLength-1);

        // Initial report set
        List<Report> reports = reportRepository.findByReportDateBetweenOrderByIdAsc(startDate, currentDate).collect(Collectors.toList());

        do {
            logger.info("Calculating histogram report for end date {} with window size {}", currentDate, windowLength);

            createHistogramReport(reports, currentDate);



            // Reset for next window
            currentDate = currentDate.plusDays(1);
            if (currentDate.isAfter(endDate)) {
                break;
            }
            reports.remove(0);
            reports.add(reportRepository.findByReportDate(currentDate).get());
        } while (!currentDate.equals(endDate));
    }

    public HistogramReport createHistogramReport(LocalDate reportDate, Integer numDays) {
        return createHistogramReport(reportDate.minusDays(numDays), reportDate);
    }

    public HistogramReport createHistogramReport(LocalDate startDate, LocalDate endDate) {
        List<Report> reports = reportRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList());

        return createHistogramReport(reports, endDate);
    }

    private HistogramReport createHistogramReport(List<Report> reports, LocalDate endDate) {
        HistogramReport histogramReport = new HistogramReport(reports);

        try {
            histogramReportRepository.save(histogramReport);
        } catch (IOException e) {
            logger.error("Error saving histogram report for report date " + endDate, e);
        }

        return histogramReport;
    }
}

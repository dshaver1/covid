package org.dshaver.covid.service;

import org.dshaver.covid.dao.AggregateReportRepository;
import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AggregateReportFactory {
    private static final Logger logger = LoggerFactory.getLogger(HistogramReportFactory.class);
    private final ReportRepository reportRepository;
    private final HistogramReportRepository histogramReportRepository;
    private final AggregateReportRepository aggregateReportRepository;

    @Inject
    public AggregateReportFactory(ReportRepository reportRepository,
                                  HistogramReportRepository histogramReportRepository,
                                  AggregateReportRepository aggregateReportRepository) {
        this.reportRepository = reportRepository;
        this.histogramReportRepository = histogramReportRepository;
        this.aggregateReportRepository = aggregateReportRepository;
    }

    public void createAllAggregateReports(LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            createAggregateReport(currentDate);
            currentDate = currentDate.plusDays(1);
        }
    }

    public AggregateReport createAggregateReport(LocalDate reportDate) {
        Optional<HistogramReportContainer> histogramReport = histogramReportRepository.findByReportDate(reportDate);
        Optional<Report> report = reportRepository.findByReportDate(reportDate);

        if (histogramReport.isPresent() && report.isPresent()) {
            return createAggregateReport(histogramReport.get(), report.get());
        }

        return null;
    }

    public AggregateReport createAggregateReport(HistogramReportContainer histogramReport, Report report) {
        AggregateReport aggregateReport = new AggregateReport();
        aggregateReport.setId(histogramReport.getId());
        aggregateReport.setReportDate(histogramReport.getReportDate());
        aggregateReport.setDaysTo90PercentCases(histogramReport.getCountyHistogramMap().values().stream()
                .map(hist -> {
                    int sum = IntStream.of(hist.getCasesHist()).sum();
                    if (sum > 100) {
                        for (int i = 0; i < 100; i++) {
                            if (hist.getCasesPercentageCumulative()[i].doubleValue() >= 90D) {
                                return new IntCountyValuePair(hist.getCounty(), i);
                            }
                        }
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(IntCountyValuePair::value).reversed()))));

        aggregateReport.setBreaches(report.getEpicurves().values().stream().flatMap(epicurve -> {
            List<EpicurvePoint> points = new LinkedList<>(epicurve.getData());
            HistogramReportV2 countyHistogramReport = histogramReport.getCountyHistogramMap().get(epicurve.getCounty());
            List<EpicurvePoint> countyBreaches = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                EpicurvePoint currentPoint = points.get(i);
                int currentMax = countyHistogramReport.getCasesMax()[i];
                int diff = currentPoint.getCasesVm() - currentMax;
                if (diff > 5) {
                    countyBreaches.add(currentPoint);
                }
            }

            return countyBreaches.stream();
        }).collect(Collectors.toList()));
        try {
            aggregateReportRepository.save(aggregateReport);
        } catch (IOException e) {
            logger.error("Error saving aggregate report for report date " + histogramReport.getReportDate(), e);
        }

        return aggregateReport;
    }
}

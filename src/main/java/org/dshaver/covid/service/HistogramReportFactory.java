package org.dshaver.covid.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.dshaver.covid.dao.HistogramReportRepository;
import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.HistogramReportContainer;
import org.dshaver.covid.domain.HistogramReportV2;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.dshaver.covid.dao.BaseFileRepository.idFormatter;
import static org.dshaver.covid.domain.HistogramReportV2.HIST_SIZE;

@Service
public class HistogramReportFactory {
    public static final MathContext DECIMALS_2 = new MathContext(2);
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

    public HistogramReportContainer createHistogramReport(LocalDate reportDate, Integer numDays) {
        return createHistogramReport(reportDate.minusDays(numDays), reportDate);
    }

    public HistogramReportContainer createHistogramReport(LocalDate startDate, LocalDate endDate) {
        List<Report> reports = reportRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList());

        return createHistogramReport(reports, endDate);
    }

    private HistogramReportContainer createHistogramReport(List<Report> reports, LocalDate endDate) {
        HistogramReportContainer histogramReport = new HistogramReportContainer();
        histogramReport.setId(LocalDateTime.now().format(idFormatter));
        histogramReport.setReportDate(endDate);
        histogramReport.setWindowSize(reports.size());

        List<String> countyList = reports.get(reports.size()-1).getEpicurves().keySet().stream().sorted().collect(Collectors.toList());
        Map<String, HistogramReportV2> reportMap = new LinkedHashMap<>(countyList.size());

        for (String currentCounty : countyList) {
            reportMap.put(currentCounty, createHistogramReportForCounty(reports, currentCounty));
        }

        histogramReport.setCountyHistogramMap(reportMap);

        try {
            histogramReportRepository.save(histogramReport);
        } catch (IOException e) {
            logger.error("Error saving histogram report for report date " + endDate, e);
        }

        return histogramReport;
    }

    private HistogramReportV2 createHistogramReportForCounty(List<Report> reports, String county) {
        HistogramReportV2 reportV2 = new HistogramReportV2(county);

        // Containers to calculate median values
        Multimap<Integer, Integer> caseDeltaMultimap = ArrayListMultimap.create();
        Multimap<Integer, Integer> deathDeltaMultimap = ArrayListMultimap.create();

        // Calculate basic case and death histogram.
        reports.stream().map(Report::getEpicurves).map(map -> map.get(county)).forEach(epicurve -> {
            Integer[] reversedCaseDeltas = getReversedValues(epicurve, EpicurvePoint::getCasesVm);
            Integer[] reversedDeathDeltas = getReversedValues(epicurve, EpicurvePoint::getDeathsVm);
            for (int i = 0; i < HIST_SIZE; i++) {
                // Sum up 0-baselined case and death deltas
                reportV2.getCasesHist()[i] = reversedCaseDeltas[i] + reportV2.getCasesHist()[i];
                reportV2.getDeathsHist()[i] = reversedDeathDeltas[i] + reportV2.getDeathsHist()[i];

                // Save off the 0-baselined values for later
                caseDeltaMultimap.put(i, reversedCaseDeltas[i]);
                deathDeltaMultimap.put(i, reversedDeathDeltas[i]);
            }
        });

        // Prepare for calculating percentage histograms
        int casesSum = IntStream.of(reportV2.getCasesHist()).sum();
        int deathsSum = IntStream.of(reportV2.getDeathsHist()).sum();

        for (int i = 0; i < HIST_SIZE; i++) {
            // Calculate median histograms
            reportV2.getCasesMedianHist()[i] = caseDeltaMultimap.get(i).stream().sorted().skip(reports.size()/2).findFirst().orElse(0);
            reportV2.getDeathsMedianHist()[i] = deathDeltaMultimap.get(i).stream().sorted().skip(reports.size()/2).findFirst().orElse(0);

            // Calculate percentage histograms
            double casePercent = ((0D + reportV2.getCasesHist()[i]) / casesSum) * 100;
            double deathPercent = ((0D + reportV2.getDeathsHist()[i]) / deathsSum) * 100;
            reportV2.getCasesPercentageHist()[i] = Double.isNaN(casePercent) || Double.isInfinite(casePercent) ? BigDecimal.ZERO : BigDecimal.valueOf(casePercent).round(DECIMALS_2);
            reportV2.getDeathsPercentageHist()[i] = Double.isNaN(deathPercent) || Double.isInfinite(deathPercent) ? BigDecimal.ZERO : BigDecimal.valueOf(deathPercent).round(DECIMALS_2);

            // Calculate cumulative percentage histograms
            reportV2.getCasesPercentageCumulative()[i] = Arrays.stream(reportV2.getCasesPercentageHist()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            reportV2.getDeathsPercentageCumulative()[i] = Arrays.stream(reportV2.getDeathsPercentageHist()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        }

        return reportV2;
    }

    private Integer[] getReversedValues(Epicurve epicurve, Function<EpicurvePoint, Integer> valueFunction) {
        return epicurve.getData().stream()
                .sorted(Comparator.comparing(EpicurvePoint::getLabelDate).reversed())
                .map(valueFunction::apply)
                .map(Math::abs)
                .limit(HIST_SIZE)
                .collect(Collectors.toList()).toArray(new Integer[HIST_SIZE]);
    }
}

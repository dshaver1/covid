package org.dshaver.covid.domain;

import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Histogram of how many days prior to topday new cases or deaths are added.
 */
@Data
public class HistogramReport {
    private static final MathContext DECIMALS_4 = new MathContext(4);

    Map<Integer, Integer> casesHist, deathsHist;
    Map<Integer, BigDecimal> casesPercentageHist, deathsPercentageHist, casesPercentageCumulative, deathsPercentageCumulative;

    public HistogramReport(Collection<Report> dailyReports) {
        casesHist = new HashMap<>();
        deathsHist = new HashMap<>();
        casesPercentageHist = new HashMap<>();
        deathsPercentageHist = new HashMap<>();
        casesPercentageCumulative = new HashMap<>();
        deathsPercentageCumulative = new HashMap<>();

        // need array to manipulate index
        Report[] reportArray = dailyReports.toArray(new Report[]{});

        // Calculate raw histograms
        for (int index = 1; index < reportArray.length; index++) {
            Report currentReport = reportArray[index];
            Report prevReport = reportArray[index - 1];
            Map<LocalDate, Integer> casesVm = getVm(currentReport, prevReport, EpicurvePoint::getPositiveCount);
            casesVm.forEach((date, value) -> {
                Integer dayDiff = ((Long) ChronoUnit.DAYS.between(currentReport.getReportDate(), date)).intValue();
                casesHist.merge(dayDiff, value, Integer::sum);
            });
            Map<LocalDate, Integer> deathsVm = getVm(currentReport, prevReport, EpicurvePoint::getDeathCount);
            deathsVm.forEach((date, value) -> {
                Integer dayDiff = ((Long) ChronoUnit.DAYS.between(currentReport.getReportDate(), date)).intValue();
                deathsHist.merge(dayDiff, value, Integer::sum);
            });
        }

        // Calculate percentage histograms
        int casesSum = casesHist.values().stream().mapToInt(i -> i).sum();
        int deathsSum = deathsHist.values().stream().mapToInt(i -> i).sum();

        casesHist.forEach((key, value) -> casesPercentageHist.put(key, BigDecimal.valueOf(((0D + value) / casesSum) * 100).round(DECIMALS_4)));
        deathsHist.forEach((key, value) -> deathsPercentageHist.put(key, BigDecimal.valueOf(((0D + value) / deathsSum) * 100).round(DECIMALS_4)));

        BigDecimal accumulator = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : casesPercentageHist.entrySet()) {
            Integer currentKey = entry.getKey();
            BigDecimal currentValue = entry.getValue();
            accumulator = accumulator.add(currentValue);
            casesPercentageCumulative.put(currentKey, accumulator);
        }

        accumulator = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : deathsPercentageHist.entrySet()) {
            Integer currentKey = entry.getKey();
            BigDecimal currentValue = entry.getValue();
            accumulator = accumulator.add(currentValue);
            deathsPercentageCumulative.put(currentKey, accumulator);
        }
    }

    private Map<LocalDate, Integer> getVm(Report currentReport, Report previousReport, Function<EpicurvePoint, Integer> valueFunction) {
        Map<LocalDate, Integer> vm = new HashMap<>();
        for (EpicurvePoint epicurvePoint : currentReport.getEpicurve()) {
            LocalDate currentDate = LocalDate.parse(epicurvePoint.getLabel(), DateTimeFormatter.ISO_LOCAL_DATE);
            Integer currentValue = valueFunction.apply(epicurvePoint);
            Optional<Integer> previousValue = previousReport.getEpicurve()
                    .stream()
                    .filter(point -> LocalDate.parse(point.getLabel(), DateTimeFormatter.ISO_LOCAL_DATE).equals(currentDate))
                    .map(valueFunction)
                    .findFirst();

            if (previousValue.isPresent()) {
                vm.put(currentDate, currentValue - previousValue.get());
            } else {
                vm.put(currentDate, currentValue);
            }
        }

        return vm;
    }
}

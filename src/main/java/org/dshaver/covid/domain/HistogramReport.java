package org.dshaver.covid.domain;

import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Histogram of how many days prior to topday new cases or deaths are added.
 */
@Data
@Document("histogramreports")
public class HistogramReport {
    private static final MathContext DECIMALS_4 = new MathContext(4);

    @Id
    String id;

    Map<Integer, Integer> casesHist, deathsHist, casesMedianHist, deathsMedianHist;
    Map<Integer, BigDecimal> casesPercentageHist, deathsPercentageHist, casesPercentageCumulative, deathsPercentageCumulative;

    public HistogramReport() {

    }

    public HistogramReport(Collection<Report> dailyReports) {
        id = dailyReports.stream().skip(dailyReports.size() - 1).map(Report::getId).findFirst().get();
        casesHist = new HashMap<>();
        deathsHist = new HashMap<>();
        casesMedianHist = new HashMap<>();
        deathsMedianHist = new HashMap<>();
        casesPercentageHist = new HashMap<>();
        deathsPercentageHist = new HashMap<>();
        casesPercentageCumulative = new HashMap<>();
        deathsPercentageCumulative = new HashMap<>();

        // need array to manipulate index
        Report[] reportArray = dailyReports.toArray(new Report[]{});

        Map<Integer, List<Integer>> casesMedianContainer = new HashMap<>();
        Map<Integer, List<Integer>> deathsMedianContainer = new HashMap<>();

        // Calculate raw histograms
        for (int index = 1; index < reportArray.length; index++) {
            Report currentReport = reportArray[index];
            Report prevReport = reportArray[index - 1];
            Map<LocalDate, Integer> casesVm = getVm(currentReport, prevReport, EpicurvePoint::getPositiveCount);
            casesVm.forEach((date, value) -> {
                Integer dayDiff = ((Long) ChronoUnit.DAYS.between(currentReport.getReportDate(), date)).intValue();
                casesHist.merge(dayDiff, value, Integer::sum);
                casesMedianContainer.computeIfAbsent(dayDiff, k -> new ArrayList<>()).add(value);
            });
            Map<LocalDate, Integer> deathsVm = getVm(currentReport, prevReport, EpicurvePoint::getDeathCount);
            deathsVm.forEach((date, value) -> {
                Integer dayDiff = ((Long) ChronoUnit.DAYS.between(currentReport.getReportDate(), date)).intValue();
                deathsHist.merge(dayDiff, value, Integer::sum);
                deathsMedianContainer.computeIfAbsent(dayDiff, k -> new ArrayList<>()).add(value);
            });
        }

        // Calculate medians
        casesMedianContainer.forEach((dayDiff, container) -> {
            Collections.sort(container);
            casesMedianHist.put(dayDiff, container.get(container.size() / 2));
        });

        deathsMedianContainer.forEach((dayDiff, container) -> {
            Collections.sort(container);
            deathsMedianHist.put(dayDiff, container.get(container.size() / 2));
        });

        // Calculate percentage histograms
        int casesSum = casesHist.values().stream().mapToInt(i -> i).sum();
        int deathsSum = deathsHist.values().stream().mapToInt(i -> i).sum();

        casesHist.forEach((key, value) -> casesPercentageHist.put(key, BigDecimal.valueOf(((0D + value) / casesSum) * 100).round(DECIMALS_4)));
        deathsHist.forEach((key, value) -> deathsPercentageHist.put(key, BigDecimal.valueOf(((0D + value) / deathsSum) * 100).round(DECIMALS_4)));

        BigDecimal accumulator = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : casesPercentageHist.entrySet().stream().sorted((e1,e2) -> e2.getKey().compareTo(e1.getKey())).collect(Collectors.toList())) {
            Integer currentKey = entry.getKey();
            BigDecimal currentValue = entry.getValue();
            accumulator = accumulator.add(currentValue);
            casesPercentageCumulative.put(currentKey, accumulator);
        }

        accumulator = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : deathsPercentageHist.entrySet().stream().sorted((e1,e2) -> e2.getKey().compareTo(e1.getKey())).collect(Collectors.toList())) {
            Integer currentKey = entry.getKey();
            BigDecimal currentValue = entry.getValue();
            accumulator = accumulator.add(currentValue);
            deathsPercentageCumulative.put(currentKey, accumulator);
        }
    }

    private Map<LocalDate, Integer> getVm(Report currentReport, Report previousReport, Function<EpicurvePoint, Integer> valueFunction) {
        Map<LocalDate, Integer> vm = new HashMap<>();
        for (EpicurvePoint epicurvePoint : currentReport.getGeorgiaEpicurve().getData()) {
            LocalDate currentDate = LocalDate.parse(epicurvePoint.getLabel(), DateTimeFormatter.ISO_LOCAL_DATE);
            Integer currentValue = valueFunction.apply(epicurvePoint);
            Optional<Integer> previousValue = previousReport.getGeorgiaEpicurve().getData()
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

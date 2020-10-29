package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
public class CountyRankReport {

    @JsonInclude(NON_EMPTY)
    private IntCountyValuePair[] intCountyRanks;

    @JsonInclude(NON_EMPTY)
    private DoubleCountyValuePair[] doubleCountyRanks;

    public CountyRankReport() {
    }

    public CountyRankReport(HistogramReportContainer histogramReportContainer) {
        doubleCountyRanks = histogramReportContainer.countyHistogramMap.values().stream()
                .filter(countyReport -> Arrays.stream(countyReport.getCasesMedianHist()).sum() > 2)
                .map(countyReport -> new DoubleCountyValuePair(countyReport.getCounty(), countyReport.getCasesPercentageCumulative()[13].doubleValue()))
                .sorted(Comparator.comparing(DoubleCountyValuePair::value).reversed())
                .collect(Collectors.toList())
                .toArray(new DoubleCountyValuePair[]{});
    }

    public CountyRankReport(Report report, Boolean prelimToggle) {
        LocalDate cutoffDate = report.getReportDate().minusDays(14);

        Predicate<EpicurvePoint> dateFilter = prelimToggle ?
                epicurvePoint -> LocalDate.parse(epicurvePoint.getTestDate()).isBefore(cutoffDate) :
                epicurvePoint -> LocalDate.parse(epicurvePoint.getTestDate()).isAfter(cutoffDate);

        intCountyRanks = report.getEpicurves().values()
                .stream()
                .map(epicurve -> new IntCountyValuePair(epicurve.getCounty(), epicurve.getData()
                        .stream()
                        .filter(dateFilter)
                        .mapToInt(EpicurvePoint::getCasesVm)
                        .map(Math::abs)
                        .sum()))
                .sorted(Comparator.comparing(IntCountyValuePair::value).reversed())
                .collect(Collectors.toList())
                .toArray(new IntCountyValuePair[]{});

    }
}

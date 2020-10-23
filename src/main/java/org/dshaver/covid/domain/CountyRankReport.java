package org.dshaver.covid.domain;

import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Data
public class CountyRankReport {
    private CountyValuePair[] countyRanks;

    public CountyRankReport() {
    }

    public CountyRankReport(Report report, Boolean prelimToggle) {
        LocalDate cutoffDate = report.getReportDate().minusDays(14);

        Predicate<EpicurvePoint> dateFilter = prelimToggle ?
                epicurvePoint -> LocalDate.parse(epicurvePoint.getTestDate()).isBefore(cutoffDate) :
                epicurvePoint -> LocalDate.parse(epicurvePoint.getTestDate()).isAfter(cutoffDate);

        countyRanks = report.getEpicurves().values()
                .stream()
                .map(epicurve -> new CountyValuePair(epicurve.getCounty(), epicurve.getData()
                        .stream()
                        .filter(dateFilter)
                        .mapToInt(EpicurvePoint::getCasesVm)
                        .map(Math::abs)
                        .sum()))
                .sorted(Comparator.comparing(CountyValuePair::getValue).reversed())
                .collect(Collectors.toList())
                .toArray(new CountyValuePair[]{});

/*        countyRanks = report.getCountyOverviewMap().values()
                .stream()
                .map(o -> new CountyValuePair(o.getCountyName(), o.getPositiveVm()))
                .sorted(Comparator.comparing(CountyValuePair::getValue).reversed())
                .collect(Collectors.toList())
                .toArray(new CountyValuePair[]{});*/
    }
}

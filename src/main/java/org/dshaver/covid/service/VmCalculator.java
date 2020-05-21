package org.dshaver.covid.service;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VmCalculator {
    public static Report populateVm(Report report, Report previousReport) {
        if (previousReport == null) {
            report.getGeorgiaEpicurve().getData().forEach(point -> {
                point.setCasesVm(0);
                point.setDeathsVm(0);
            });

            return report;
        }

        Table<String, LocalDate, Integer> casesVm = getCurveVm(report, previousReport, EpicurvePoint::getPositiveCount);
        Table<String, LocalDate, Integer> deathsVm = getCurveVm(report, previousReport, EpicurvePoint::getDeathCount);
        report.getEpicurves().values().stream().flatMap(epicurve -> epicurve.getData().stream()).forEach(point -> {
            point.setCasesVm(casesVm.get(point.getCounty(), point.getLabelDate()));
            point.setDeathsVm(deathsVm.get(point.getCounty(), point.getLabelDate()));
        });

        return report;
    }

    public static Table<String, LocalDate, Integer> getCurveVm(Report currentReport, Report previousReport, Function<EpicurvePoint, Integer> valueFunction) {
        Table<String, LocalDate, Integer> vm = TreeBasedTable.create();
        for (String currentCounty : currentReport.getEpicurves().keySet()) {
            for (EpicurvePoint epicurvePoint : currentReport.getEpicurves().get(currentCounty).getData()) {
                LocalDate currentDate = epicurvePoint.getLabelDate();
                Integer currentValue = valueFunction.apply(epicurvePoint);
                Optional<Epicurve> maybePreviousEpicurve = Optional.ofNullable(previousReport.getEpicurves().get(currentCounty));
                Optional<Integer> previousValue = Optional.empty();
                if (maybePreviousEpicurve.isPresent()) {
                    previousValue = previousReport.getEpicurves().get(currentCounty).getData()
                            .stream()
                            .filter(point -> point.getLabelDate().equals(currentDate))
                            .map(valueFunction)
                            .findFirst();
                }

                if (previousValue.isPresent()) {
                    vm.put(currentCounty, currentDate, currentValue - previousValue.get());
                } else {
                    vm.put(currentCounty, currentDate, currentValue);
                }
            }
        }

        return vm;
    }

    /**
     * Calculates the top 10 highest caseVM values across all counties, excluding "Georgia" since that is aggregate.
     * <p>
     * Assumes that VM has already been calculated.
     */
    public static Report populateBiggestCaseDeltas(Report report) {
        Set<EpicurvePoint> top10 = report.getEpicurves().values()
                .stream()
                .filter(epicurve -> !epicurve.getCounty().equals("Georgia"))
                .flatMap(epicurve -> epicurve.getData().stream())
                .sorted(Comparator.comparing(EpicurvePoint::getCasesVm).reversed())
                .limit(5)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(EpicurvePoint::getCasesVm).reversed())));

        report.setTop5CaseDeltas(top10);

        return report;
    }
}

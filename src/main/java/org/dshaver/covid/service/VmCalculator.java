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
    public static final LocalDate CUTOFF_DATE = LocalDate.of(2020, 4,15);

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
            point.setCasesVm(casesVm.get(point.getCounty().toLowerCase(), point.getLabelDate()));
            point.setDeathsVm(deathsVm.get(point.getCounty().toLowerCase(), point.getLabelDate()));
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
                } else if (currentDate.isAfter(CUTOFF_DATE)) {
                    vm.put(currentCounty, currentDate, currentValue);
                } else {
                    vm.put(currentCounty, currentDate, 0);
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
    public static Collection<EpicurvePoint> calculateTopDeltas(Report report, Function<EpicurvePoint, Integer> valueFunction) {
        List<EpicurvePoint> sorted = report.getEpicurves().values()
                .stream()
                .filter(epicurve -> !epicurve.getCounty().toLowerCase().equals("georgia"))
                .filter(epicurve -> !epicurve.getCounty().toLowerCase().equals("unknown"))
                .flatMap(epicurve -> epicurve.getData().stream())
                .filter(point -> valueFunction.apply(point) != null)
                .filter(point -> point.getLabel() != null)
                .sorted(Comparator.comparing(valueFunction).thenComparing(EpicurvePoint::getLabel).reversed())
                .collect(Collectors.toList());

        // Short circuit if no county data (most cases at the moment)
        if (sorted.isEmpty()) {
            return new ArrayList<>();
        }

        int topX = 5;
        EpicurvePoint[] array = new EpicurvePoint[topX];
        Iterator<EpicurvePoint> iterator = sorted.iterator();
        for (int i = 0; i < topX; i++) {
            array[i] = iterator.next();
        }

        return Arrays.asList(array);
    }
}

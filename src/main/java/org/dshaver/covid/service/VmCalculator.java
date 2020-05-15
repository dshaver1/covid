package org.dshaver.covid.service;

import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class VmCalculator {
    public static Report populateVm(Report report, Report previousReport) {
        if (previousReport == null) {
            report.getGeorgiaEpicurve().getData().forEach(point -> {
                point.setCasesVm(0);
                point.setDeathsVm(0);
            });

            return report;
        }

        Map<LocalDate, Integer> casesVm = getCurveVm(report, previousReport, EpicurvePoint::getPositiveCount);
        Map<LocalDate, Integer> deathsVm = getCurveVm(report, previousReport, EpicurvePoint::getDeathCount);
        report.getGeorgiaEpicurve().getData().forEach(point -> {
            point.setCasesVm(casesVm.get(point.getLabelDate()));
            point.setDeathsVm(deathsVm.get(point.getLabelDate()));
        });

        return report;
    }

    public static Map<LocalDate, Integer> getCurveVm(Report currentReport, Report previousReport, Function<EpicurvePoint, Integer> valueFunction) {
        Map<LocalDate, Integer> vm = new HashMap<>();
        for (EpicurvePoint epicurvePoint : currentReport.getGeorgiaEpicurve().getData()) {
            LocalDate currentDate = epicurvePoint.getLabelDate();
            Integer currentValue = valueFunction.apply(epicurvePoint);
            Optional<Integer> previousValue = previousReport.getGeorgiaEpicurve().getData()
                    .stream()
                    .filter(point -> point.getLabelDate().equals(currentDate))
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

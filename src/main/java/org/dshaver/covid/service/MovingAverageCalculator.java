package org.dshaver.covid.service;

import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class MovingAverageCalculator {
    public static Report calculate(Report report, int numDays) {
        report.getEpicurves().keySet().forEach(county -> calculate(report, numDays, county));

        return report;
    }

    public static Report calculate(Report report, int numDays, String county) {
        Collection<EpicurvePoint> lookupCurve = new ArrayList<>(report.getEpicurves().get(county).getData());

        report.getEpicurves().get(county).getData().forEach(p -> {
            p.setMovingAvg((int) lookupCurve.stream()
                    .filter(lookupPoint -> !p.getLabelDate().isBefore(lookupPoint.getLabelDate()))
                    .sorted(Comparator.comparing(EpicurvePoint::getLabelDate).reversed())
                    .limit(numDays)
                    .mapToInt(EpicurvePoint::getPositiveCount)
                    .average()
                    .orElse(0D));
        });

        return report;
    }
}

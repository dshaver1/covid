package org.dshaver.covid.service;

import org.dshaver.covid.dao.ReportRepository;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class EpicurveAggregatorTools {

    private final ReportRepository reportRepository;

    @Inject
    public EpicurveAggregatorTools(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Report populateRollingSum(Report currentReport) {
        Epicurve epicurve = currentReport.getGeorgiaEpicurve();
        EpicurvePoint[] pointArray = epicurve.getData().toArray(new EpicurvePoint[0]);

        // First 7 points need special handling. We want to count the current date multiple times in order to sidestep
        // not having enough data points at the start of the curve.
        for (int i = 0; i < 7; i++) {
            EpicurvePoint currentPoint = pointArray[i];
            int adjustedSum = currentPoint.getPositiveCount() * (7 - i);
            for (int j = i - 1; j >= 0; j--) {
                adjustedSum += pointArray[j].getPositiveCount();
            }
            currentPoint.setRollingCases(adjustedSum);
        }

        // Now continue on, summing a trailing 7 days.
        for (int i = 7; i < pointArray.length; i++) {
            EpicurvePoint currentPoint = pointArray[i];
            int sum = currentPoint.getPositiveCount();
            for (int j = 1; j < 7; j++) {
                sum += pointArray[i - j].getPositiveCount();
            }
            currentPoint.setRollingCases(sum);
        }

        epicurve.setData(Arrays.asList(pointArray));

        return currentReport;
    }
}

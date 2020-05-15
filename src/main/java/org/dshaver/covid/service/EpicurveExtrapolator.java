package org.dshaver.covid.service;

import org.dshaver.covid.domain.HistogramReport;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Sets the {@link EpicurvePoint#setCasesExtrapolated(Integer)}
 */
public class EpicurveExtrapolator {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);

    public static Report extrapolateCases(Report report, HistogramReport histogramReport) {
        if (histogramReport == null) {
            return report;
        }

        EpicurvePoint[] epicurveArray = report.getGeorgiaEpicurve().getData().toArray(new EpicurvePoint[0]);
        Map<Integer, BigDecimal> casesPercentageCumulative = histogramReport.getCasesPercentageCumulative();
        for (int idx = 0; idx < epicurveArray.length; idx++) {
            // What is the percentage of adds that occur on the given date?
            int reverseIdx = reverseIdx(idx, epicurveArray.length);
            BigDecimal currentHist = casesPercentageCumulative.get(reverseIdx).divide(HUNDRED, RoundingMode.HALF_EVEN);
            EpicurvePoint currentPoint = epicurveArray[idx];
            // Multiply the current value by the inverse of the cumulative percentage.
            BigDecimal currentPointPositiveCount = BigDecimal.valueOf(currentPoint.getPositiveCount());
            BigDecimal divided = currentPointPositiveCount.divide(currentHist, RoundingMode.HALF_DOWN);
            currentPoint.setCasesExtrapolated(divided.intValue());
        }

        return report;
    }

    /**
     * Assuming a length of 88
     *
     * 0 -> -87
     * 1 -> -86
     * ...
     * 86 -> -1
     * 87 -> 0
     * @param idx
     * @param length
     * @return
     */
    private static int reverseIdx(int idx, int length) {
        return -1 * (length - 1 - idx);
    }
}

package org.dshaver.covid.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;

@Data
public class HistogramReportV2 {
    public static final int HIST_SIZE = 100;

    private String county;
    private final int[] casesMin = new int[HIST_SIZE], casesMax = new int[HIST_SIZE], casesHist = new int[HIST_SIZE], deathsHist = new int[HIST_SIZE], casesMedianHist = new int[HIST_SIZE], deathsMedianHist = new int[HIST_SIZE];
    private final BigDecimal[] casesPercentageHist = new BigDecimal[HIST_SIZE], deathsPercentageHist = new BigDecimal[HIST_SIZE], casesPercentageCumulative = new BigDecimal[HIST_SIZE], deathsPercentageCumulative = new BigDecimal[HIST_SIZE];

    public HistogramReportV2() {
    }

    public HistogramReportV2(String county) {
        this.county = county;

        Arrays.fill(casesHist, 0);
        Arrays.fill(deathsHist, 0);
        Arrays.fill(casesMedianHist, 0);
        Arrays.fill(deathsMedianHist, 0);
        Arrays.fill(casesMin, 0);
        Arrays.fill(casesMax, 0);
        Arrays.fill(casesPercentageHist, BigDecimal.ZERO);
        Arrays.fill(deathsPercentageHist, BigDecimal.ZERO);
        Arrays.fill(casesPercentageCumulative, BigDecimal.ZERO);
        Arrays.fill(deathsPercentageCumulative, BigDecimal.ZERO);
    }
}

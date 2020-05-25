package org.dshaver.covid.domain.epicurve;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EpicurvePoint extends Comparable<EpicurvePoint> {
    LocalDate getLabelDate();

    void setLabelDate(LocalDate labelDate);

    String getLabel();

    void setLabel(String label);

    String getCounty();

    void setCounty(String county);

    String getSource();

    void setSource(String source);

    String getTestDate();

    void setTestDate(String testDate);

    Integer getPositiveCount();

    void setPositiveCount(Integer positiveCount);

    Integer getDeathCount();

    void setDeathCount(Integer deathCount);

    Integer getPositivesCumulative();

    void setPositivesCumulative(Integer positivesCumulative);

    Integer getDeathsCumulative();

    void setDeathsCumulative(Integer deathsCumulative);

    Integer getCasesVm();

    void setCasesVm(Integer casesVm);

    Integer getDeathsVm();

    void setDeathsVm(Integer deathsVm);

    Integer getCasesExtrapolated();

    void setCasesExtrapolated(Integer casesExtrapolated);

    Integer getMedianCaseDelta();

    void setMedianCaseDelta(Integer medianCaseDelta);

    Integer getMovingAvg();

    void setMovingAvg(Integer movingAvg);
}

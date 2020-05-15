package org.dshaver.covid.domain.epicurve;

public interface EpicurvePoint extends Comparable<EpicurvePoint> {
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
}

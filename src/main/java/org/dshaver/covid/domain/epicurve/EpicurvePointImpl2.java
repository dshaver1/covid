package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

public class EpicurvePointImpl2 extends BaseEpicurvePoint {

    public EpicurvePointImpl2() {

    }

    public EpicurvePointImpl2(EpicurvePointImpl2 original) {
        this.labelDate = original.getLabelDate();
        this.label = original.getLabel();
        this.source = original.getSource();
        this.county = original.getCounty();
        this.testDate = original.getTestDate();
        this.positiveCount = original.getPositiveCount();
        this.deathCount = original.getDeathCount();
        this.positivesCumulative = original.getPositivesCumulative();
        this.deathsCumulative = original.getDeathsCumulative();
        this.casesVm = original.getCasesVm();
        this.deathsVm = original.getDeathsVm();
        this.casesExtrapolated = original.getCasesExtrapolated();
    }

    @JsonGetter("county")
    public String getCounty() {
        return county;
    }

    @JsonSetter("county")
    public void setCounty(String county) {
        this.county = county;
    }

    @JsonGetter("test_date")
    public String getTestDate() {
        return testDate;
    }

    @JsonSetter("test_date")
    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    @JsonGetter("positives")
    public Integer getPositiveCount() {
        return positiveCount;
    }

    @JsonSetter("positives")
    public void setPositiveCount(Integer positiveCount) {
        this.positiveCount = positiveCount;
    }

    @JsonGetter("deathcnt")
    public Integer getDeathCount() {
        return deathCount;
    }

    @JsonSetter("deathcnt")
    public void setDeathCount(Integer deathCount) {
        this.deathCount = deathCount;
    }

    @JsonGetter("positives_cum")
    public Integer getPositivesCumulative() {
        return positivesCumulative;
    }

    @JsonSetter("positives_cum")
    public void setPositivesCumulative(Integer positivesCumulative) {
        this.positivesCumulative = positivesCumulative;
    }

    @JsonGetter("death_cum")
    public Integer getDeathsCumulative() {
        return deathsCumulative;
    }

    @JsonSetter("death_cum")
    public void setDeathsCumulative(Integer deathsCumulative) {
        this.deathsCumulative = deathsCumulative;
    }
}

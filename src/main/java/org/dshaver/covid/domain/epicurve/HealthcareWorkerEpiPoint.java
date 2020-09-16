package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

public class HealthcareWorkerEpiPoint extends BaseEpicurvePoint {

    public HealthcareWorkerEpiPoint() {
    }

    public HealthcareWorkerEpiPoint(String county) {
        this.county = county;
        this.positiveCount = 0;
        this.deathCount = 0;
    }

    public HealthcareWorkerEpiPoint(HealthcareWorkerEpiPoint original) {
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

    @JsonGetter("county_name")
    public String getCounty() {
        return county;
    }

    @JsonSetter("county_name")
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

    @JsonGetter("deaths")
    public Integer getDeathCount() {
        return deathCount;
    }

    @JsonSetter("deaths")
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

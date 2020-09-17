package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

public class EpicurvePointImpl2 extends BaseEpicurvePoint {
    @JsonProperty("pcrtest")
    Integer pcrTest;
    @JsonProperty("pcrpos")
    Integer pcrPos;
    @JsonProperty("day7_per_pcrpos")
    Double day7PerPcrPos;
    Double day14PerPcrPos;

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

    @JsonProperty("pcrtest")
    public Integer getPcrTest() {
        return pcrTest;
    }

    @JsonProperty("pcrtest")
    public void setPcrTest(int pcrTest) {
        this.pcrTest = pcrTest;
    }

    @JsonProperty("pcrpos")
    public Integer getPcrPos() {
        return pcrPos;
    }

    @JsonProperty("pcrpos")
    public void setPcrPos(int pcrPos) {
        this.pcrPos = pcrPos;
    }

    @JsonProperty("day7_per_pcrpos")
    public Double getDay7PerPcrPos() {
        return day7PerPcrPos;
    }

    @JsonProperty("day7_per_pcrpos")
    public void setDay7PerPcrPos(double day7PerPcrPos) {
        this.day7PerPcrPos = day7PerPcrPos;
    }

    @JsonProperty("day14_per_pcrpos")
    public Double getDay14PerPcrPos() {
        return day14PerPcrPos;
    }

    @JsonProperty("day14_per_pcrpos")
    public void setDay14PerPcrPos(double day14PerPcrPos) {
        this.day14PerPcrPos = day14PerPcrPos;
    }
}

package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EpicurvePointImpl1 implements EpicurvePoint {
    String label;
    String source;
    @JsonProperty("test_date")
    String testDate;
    Integer positiveCount;
    @JsonProperty("deathcnt")
    Integer deathCount;
    Integer positivesCumulative;
    @JsonProperty("death_cum")
    Integer deathsCumulative;

    @Override
    public int compareTo(EpicurvePoint o) {
        return label.compareTo(o.getLabel());
    }

    @Override
    public String getCounty() {
        return "Georgia";
    }

    @Override
    public void setCounty(String county) {
        // Do nothing... epicurve v1 doesn't support counties.
    }

    @JsonProperty("positives")
    public Integer getPositiveCount() {
        return positiveCount;
    }

    @JsonProperty("POSITIVES")
    public void setPositiveCount(Integer positiveCount) {
        this.positiveCount = positiveCount;
    }

    @JsonProperty("positives_cum")
    public Integer getPositivesCumulative() {
        return positivesCumulative;
    }

    @JsonProperty("POSITIVES_CUM")
    public void setPositivesCumulative(Integer positivesCumulative) {
        this.positivesCumulative = positivesCumulative;
    }
}

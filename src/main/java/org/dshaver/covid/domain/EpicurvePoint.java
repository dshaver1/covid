package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EpicurvePoint {
    String label;
    String source;
    @JsonProperty("test_date")
    String testDate;
    @JsonProperty("POSITIVES")
    Integer positiveCount;
    @JsonProperty("deathcnt")
    Integer deathCount;
    @JsonProperty("POSITIVES_CUM")
    Integer positivesCumulative;
    @JsonProperty("death_cum")
    Integer deathsCumulative;
    @JsonProperty("moving_avg")
    BigDecimal movingAverage;
}

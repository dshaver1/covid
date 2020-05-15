package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EpicurvePointImpl2 implements EpicurvePoint {
    String label;
    String source;
    @JsonProperty("county")
    String county;
    @JsonProperty("test_date")
    String testDate;
    @JsonProperty("positives")
    Integer positiveCount;
    @JsonProperty("deathcnt")
    Integer deathCount;
    @JsonProperty("positives_cum")
    Integer positivesCumulative;
    @JsonProperty("death_cum")
    Integer deathsCumulative;

    @Override
    public int compareTo(EpicurvePoint o) {
        int compare = 0;
        compare = county.compareTo(o.getCounty());

        if (compare == 0) {
            compare = label.compareTo(o.getLabel());
        }

        return compare;
    }
}

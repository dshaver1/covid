package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
public class EpicurvePointImpl2 implements EpicurvePoint {
    LocalDate labelDate;
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
    Integer casesVm;
    Integer deathsVm;

    @Override
    public int compareTo(EpicurvePoint o) {
        int compare = 0;
        compare = county.compareTo(o.getCounty());

        if (compare == 0) {
            compare = label.compareTo(o.getLabel());
        }

        return compare;
    }

    @JsonIgnore
    public LocalDate getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(LocalDate labelDate) {
        this.labelDate = labelDate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        if (labelDate == null) {
            this.labelDate = LocalDate.parse(getLabel(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}

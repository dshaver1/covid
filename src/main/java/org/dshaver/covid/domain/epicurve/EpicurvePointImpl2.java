package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@EqualsAndHashCode
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
    Integer casesExtrapolated;
    @JsonIgnore
    Integer manualCaseTotal;
    @JsonIgnore
    Integer manualDeathTotal;

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

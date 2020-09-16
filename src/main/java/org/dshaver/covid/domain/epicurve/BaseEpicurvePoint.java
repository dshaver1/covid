package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode
public class BaseEpicurvePoint implements EpicurvePoint {
    LocalDate labelDate;
    String label;
    String source;
    String county;
    String testDate;
    Integer positiveCount;
    Integer deathCount;
    Integer positivesCumulative;
    Integer deathsCumulative;
    Integer casesVm;
    Integer deathsVm;
    Integer casesExtrapolated;
    Integer medianCaseDelta;
    Integer movingAvg;
    @JsonIgnore
    Integer manualCaseTotal;
    @JsonIgnore
    Integer manualDeathTotal;

    @Override
    public int compareTo(EpicurvePoint o) {
        int compare = 0;
        compare = county.toLowerCase().compareTo(o.getCounty().toLowerCase());

        if (compare == 0) {
            compare = label.compareTo(o.getLabel());
        }

        return compare;
    }
}

package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Data
public class Epicurve {
    @JsonProperty("SASJSONExport")
    String exportFormat;

    @JsonProperty("SASTableData+EPICURVE")
    Collection<EpicurvePoint> epicurvePoints;

    public Collection<EpicurvePoint> getEpicurvePoints() {
        if (epicurvePoints == null) {
            epicurvePoints = new TreeSet<>(Comparator.comparing(EpicurvePoint::getLabel));
        }

        return epicurvePoints;
    }

    public void setEpicurvePoints(List<EpicurvePoint> epicurvePoints) {
        this.epicurvePoints = epicurvePoints;
    }
}

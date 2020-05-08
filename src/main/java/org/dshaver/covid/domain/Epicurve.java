package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Epicurve {
    @JsonProperty("SASJSONExport")
    String exportFormat;

    Collection<EpicurvePoint> epicurvePoints;

    @JsonProperty("epicurvePoints")
    public Collection<EpicurvePoint> getEpicurvePoints() {
        if (epicurvePoints == null) {
            epicurvePoints = new TreeSet<>(Comparator.comparing(EpicurvePoint::getLabel));
        }

        return epicurvePoints;
    }

    @JsonProperty("SASTableData+EPICURVE")
    public void setEpicurvePoints(List<EpicurvePoint> epicurvePoints) {
        this.epicurvePoints = epicurvePoints;
    }


    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }
}

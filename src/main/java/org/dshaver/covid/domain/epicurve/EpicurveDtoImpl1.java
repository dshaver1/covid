package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class EpicurveDtoImpl1 implements EpicurveDto {
    private LocalDate reportDate;
    private String id;
    private Path filePath;

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

    @JsonDeserialize(contentAs = EpicurvePointImpl1.class)
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

    @JsonIgnore
    @Override
    public Multimap<String, EpicurvePoint> getAllEpicurves() {
        Multimap<String, EpicurvePoint> map = ArrayListMultimap.create();

        map.putAll("Georgia", getEpicurvePoints());

        return map;
    }

    @JsonIgnore
    @Override
    public Collection<EpicurvePoint> getEpicurveForCounty(String county) {
        if (!"Georgia".equals(county)) {
            throw new UnsupportedOperationException("Cannot get county data from epicurveV1 (yet...?)");
        }
        return getEpicurvePoints();
    }

    @JsonIgnore
    @Override
    public Collection<EpicurvePoint> getStateEpicurve() {
        return getEpicurvePoints();
    }

    @Override
    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
}

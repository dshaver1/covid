package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;


public class ManualReportRequest {
    @DateTimeFormat
    LocalDate reportDate;
    String id;
    int totalTests;
    int confirmedCases;
    int icu;
    int hospitalizations;
    int deaths;
    @JsonDeserialize(contentAs = EpicurvePointImpl2.class)
    List<EpicurvePoint> georgiaEpicurve;

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<EpicurvePoint> getGeorgiaEpicurve() {
        return georgiaEpicurve;
    }

    public void setGeorgiaEpicurve(List<EpicurvePoint> georgiaEpicurve) {
        this.georgiaEpicurve = georgiaEpicurve;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getConfirmedCases() {
        return confirmedCases;
    }

    public void setConfirmedCases(int confirmedCases) {
        this.confirmedCases = confirmedCases;
    }

    public int getIcu() {
        return icu;
    }

    public void setIcu(int icu) {
        this.icu = icu;
    }

    public int getHospitalizations() {
        return hospitalizations;
    }

    public void setHospitalizations(int hospitalizations) {
        this.hospitalizations = hospitalizations;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
}

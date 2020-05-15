package org.dshaver.covid.domain.overview;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface ReportOverview {
    Integer getTotalTests();

    Integer getConfirmedCovid();

    Integer getIcu();

    Integer getHospitalization();

    Integer getDeaths();
}

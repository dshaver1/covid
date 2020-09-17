package org.dshaver.covid.domain.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dshaver.covid.domain.Identifiable;

public interface ReportOverview extends Identifiable {
    Integer getTotalTests();

    Integer getConfirmedCovid();

    Integer getIcu();

    Integer getHospitalization();

    Integer getDeaths();
}

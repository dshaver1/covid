package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CountyOverview {

    @JsonAlias({"county_resident", "county_name"})
    private String countyName;

    @JsonAlias({"positive", "positives"})
    private Integer positive;
    private Integer positiveVm;
    private Integer population;
    private Integer hospitalization;
    private Integer deaths;
    private Integer deathsVm;
}

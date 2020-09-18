package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CountyOverview {
    @JsonProperty("county_name")
    private String countyName;
    private Integer positive;
    private Integer population;
    private Integer hospitalization;
    private Integer deaths;
    private Double rate;
    @JsonProperty("death_rate")
    private Double deathRate;
    @JsonProperty("in14day_rate")
    private Double in14DayRate;
    @JsonProperty("positives_14day")
    private Double positives14Day;
}

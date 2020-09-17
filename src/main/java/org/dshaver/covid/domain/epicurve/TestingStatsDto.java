package org.dshaver.covid.domain.epicurve;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TestingStatsDto {
    String county;
    @JsonProperty("report_date")
    LocalDate reportDate;
    @JsonProperty("pcrtest")
    int pcrTest;
    @JsonProperty("cum_pcrtest")
    int cumPcrTest;
    @JsonProperty("pcrpos")
    int pcrPos;
    @JsonProperty("cum_pcrpos")
    int cumPcrPos;
    @JsonProperty("day7_per_pcrpos")
    double day7PerPcrPos;
    @JsonProperty("day14_per_pcrpos")
    double day14PerPcrPos;
}

package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 {"SASJSONExport":"1.0 PRETTY","SASTableData+GA_COVID19_OVERALL":[{"TOTAL_TESTS":217303,"CONFIRMED_COVID":31150,"ICU":1353,"HOSPITALIZATION":5793,"DEATHS":1328}]}
 **/
@Data
public class ReportOverview {
    @JsonProperty("TOTAL_TESTS")
    Integer totalTests;
    @JsonProperty("CONFIRMED_COVID")
    Integer confirmedCovid;
    @JsonProperty("ICU")
    Integer icu;
    @JsonProperty("HOSPITALIZATION")
    Integer hospitalization;
    @JsonProperty("DEATHS")
    Integer deaths;
}

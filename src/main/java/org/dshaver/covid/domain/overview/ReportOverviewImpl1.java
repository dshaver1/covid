package org.dshaver.covid.domain.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * {"SASJSONExport":"1.0 PRETTY","SASTableData+GA_COVID19_OVERALL":[{"TOTAL_TESTS":217303,"CONFIRMED_COVID":31150,"ICU":1353,"HOSPITALIZATION":5793,"DEATHS":1328}]}
 **/
@Data
public class ReportOverviewImpl1 implements ReportOverview {
    LocalDate reportDate;
    String id;
    Path filePath;
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

package org.dshaver.covid.domain.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * JSON.parse('[{"total_tests":285881,"confirmed_covid":35858,"icu":1513,"hospitalization":6345,"deaths":1527}]')}
 **/
@Data
public class ReportOverviewImpl2 implements ReportOverview {
    LocalDate reportDate;
    String id;
    Path filePath;
    @JsonProperty("total_tests")
    Integer totalTests;
    @JsonProperty("confirmed_covid")
    Integer confirmedCovid;
    @JsonProperty("icu")
    Integer icu;
    @JsonProperty("hospitalization")
    Integer hospitalization;
    @JsonProperty("deaths")
    Integer deaths;
}

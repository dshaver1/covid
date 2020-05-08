package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
{"SASJSONExport":"1.0 PRETTY","SASTableData+GA_COVID19_OVERALL":[{"TOTAL_TESTS":217303,"CONFIRMED_COVID":31150,"ICU":1353,"HOSPITALIZATION":5793,"DEATHS":1328}]}
 **/
public class ReportOverviewContainer {
    @JsonProperty("SASJSONExport")
    String exportFormat;

    List<ReportOverview> reportOverviewList;

    @JsonProperty("reportOverviewList")
    public List<ReportOverview> getReportOverviewList() {
        return reportOverviewList;
    }

    @JsonProperty("SASTableData+GA_COVID19_OVERALL")
    public void setReportOverviewList(List<ReportOverview> reportOverviewList) {
        this.reportOverviewList = reportOverviewList;
    }
}

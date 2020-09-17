package org.dshaver.covid.domain.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dshaver.covid.domain.Identifiable;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
{"SASJSONExport":"1.0 PRETTY","SASTableData+GA_COVID19_OVERALL":[{"TOTAL_TESTS":217303,"CONFIRMED_COVID":31150,"ICU":1353,"HOSPITALIZATION":5793,"DEATHS":1328}]}
 **/
public class ReportOverviewContainer implements Identifiable {
    LocalDate reportDate;
    String id;
    Path filePath;
    @JsonProperty("SASJSONExport")
    String exportFormat;

    List<ReportOverviewImpl1> reportOverviewList;

    @JsonProperty("reportOverviewList")
    public List<ReportOverviewImpl1> getReportOverviewList() {
        return reportOverviewList;
    }

    @JsonProperty("SASTableData+GA_COVID19_OVERALL")
    public void setReportOverviewList(List<ReportOverviewImpl1> reportOverviewList) {
        this.reportOverviewList = reportOverviewList;
    }

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

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
}

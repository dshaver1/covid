package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.ToString;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * Created by xpdf64 on 2020-04-22.
 */
@Data
@ToString(exclude = {"epicurves"})
@JsonPropertyOrder(value = {"id", "createTime", "reportDate", "totalTests", "totalTestsVm", "confirmedCases",
        "confirmedCasesVm", "deaths", "deathsVm", "hospitalized", "hospitalizedVm", "icu", "icuVm",
        "top5CaseDeltas", "top5DeathDeltas", "georgiaEpicurve"})
@Document("reports")
public class Report {
    public static final String GEORGIA = "georgia";
    private static final Logger logger = LoggerFactory.getLogger(Report.class);
    private static final String dataFolder = "H:\\dev\\covid\\data\\";

    @Id
    private String id;
    private LocalDateTime createTime;
    private LocalDate reportDate;
    private Map<String, Epicurve> epicurves;
    private Collection<EpicurvePoint> top5CaseDeltas;
    private Collection<EpicurvePoint> top5DeathDeltas;
    private int totalTests;
    private int totalTestsVm;
    private int confirmedCases;
    private int confirmedCasesVm;
    private int hospitalized;
    private int hospitalizedVm;
    private int deaths;
    private int deathsVm;
    private int icu;
    private int icuVm;

    public Report() {
    }

    public Report(LocalDateTime createTime, String id, LocalDate reportDate, Map<String, Epicurve> epicurves, int totalTests, int confirmedCases, int hospitalized,
                  int deaths, int totalTestsVm, int confirmedCasesVm, int hospitalizedVm, int deathsVm) {
        this.createTime = createTime;
        this.id = id;
        this.reportDate = reportDate;
        this.totalTests = totalTests;
        this.confirmedCases = confirmedCases;
        this.hospitalized = hospitalized;
        this.deaths = deaths;
        this.epicurves = epicurves;
        this.totalTestsVm = totalTestsVm;
        this.confirmedCasesVm = confirmedCasesVm;
        this.hospitalizedVm = hospitalizedVm;
        this.deathsVm = deathsVm;
    }

    public Report(LocalDateTime createTime, String id, LocalDate reportDate, Map<String, Epicurve> epicurves, int totalTests, int confirmedCases, int hospitalized,
                  int deaths, int icu, int totalTestsVm, int confirmedCasesVm, int hospitalizedVm, int deathsVm, int icuVm) {
        this.createTime = createTime;
        this.id = id;
        this.reportDate = reportDate;
        this.totalTests = totalTests;
        this.confirmedCases = confirmedCases;
        this.hospitalized = hospitalized;
        this.deaths = deaths;
        this.epicurves = epicurves;
        this.icu = icu;
        this.totalTestsVm = totalTestsVm;
        this.confirmedCasesVm = confirmedCasesVm;
        this.hospitalizedVm = hospitalizedVm;
        this.deathsVm = deathsVm;
        this.icuVm = icuVm;
    }

    public Epicurve getGeorgiaEpicurve() {
        return epicurves.get(GEORGIA);
    }

    // Don't save georgiaEpicurve to db since that would be redundant.
    @JsonIgnore
    public void setGeorgiaEpicurve() {
        // do nothing...
    }

    // Don't print ALL epicurves to json as that would be too large.
    @JsonIgnore
    public Map<String, Epicurve> getEpicurves() {
        return epicurves;
    }

    public void setEpicurves(Map<String, Epicurve> epicurves) {
        this.epicurves = epicurves;
    }
}

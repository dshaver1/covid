package org.dshaver.covid.domain;

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
@ToString(exclude = {"epicurve"})
@Document("reports")
public class Report {
    private static final Logger logger = LoggerFactory.getLogger(Report.class);
    private static final String dataFolder = "H:\\dev\\covid\\data\\";

    @Id
    private String id;
    private LocalDateTime createTime;
    private LocalDate reportDate;
    private Collection<EpicurvePoint> epicurve;
    private int totalTests;
    private int confirmedCases;
    private int hospitalized;
    private int deaths;
    private int icu;

    public Report() {
    }

    public Report(LocalDateTime createTime, String id, LocalDate reportDate, Epicurve epicurve, int totalTests, int confirmedCases, int hospitalized,
                  int deaths) {
        this.createTime = createTime;
        this.id = id;
        this.reportDate = reportDate;
        this.totalTests = totalTests;
        this.confirmedCases = confirmedCases;
        this.hospitalized = hospitalized;
        this.deaths = deaths;
        this.epicurve = epicurve.getStateEpicurve();
    }

    public Report(LocalDateTime createTime, String id, LocalDate reportDate, Epicurve epicurve, int totalTests, int confirmedCases, int hospitalized,
                  int deaths, int icu) {
        this.createTime = createTime;
        this.id = id;
        this.reportDate = reportDate;
        this.totalTests = totalTests;
        this.confirmedCases = confirmedCases;
        this.hospitalized = hospitalized;
        this.deaths = deaths;
        this.epicurve = epicurve.getStateEpicurve();
        this.icu = icu;
    }
}

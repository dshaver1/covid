package org.dshaver.covid.domain;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.ToString;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

@Data
@ToString(exclude = {"curveDates", "caseDeltas", "deathDeltas", "cases", "deaths", "caseProjections", "movingAvgs"})
@Document("arrayreports")
public class ArrayReport {
    @Id
    private String id;
    private LocalDateTime createTime;
    private LocalDate reportDate;
    private int totalTests;
    private int totalTestsVm;
    private int totalConfirmedCases;
    private int totalDeaths;
    private int confirmedCasesVm;
    private int hospitalized;
    private int hospitalizedVm;
    private int deathsVm;
    private int icu;
    private int icuVm;
    private LocalDate[] curveDates;
    private Integer[] caseDeltas;
    private Integer[] deathDeltas;
    private Integer[] cases;
    private Integer[] deaths;
    private Integer[] caseProjections;
    private Integer[] movingAvgs;

    public ArrayReport() {

    }

    public ArrayReport(Report report) {
        this.id = report.getId();
        this.createTime = report.getCreateTime();
        this.reportDate = report.getReportDate();
        this.totalTests = report.getTotalTests();
        this.totalTestsVm = report.getTotalTestsVm();
        this.totalConfirmedCases = report.getConfirmedCases();
        this.totalDeaths = report.getDeaths();
        this.confirmedCasesVm = report.getConfirmedCasesVm();
        this.hospitalized = report.getHospitalized();
        this.hospitalizedVm = report.getHospitalizedVm();
        this.deathsVm = report.getDeathsVm();
        this.icu = report.getIcu();
        this.icuVm = report.getIcuVm();

        Collection<EpicurvePoint> dataPoints = report.getGeorgiaEpicurve().getData();
        this.curveDates = dataPoints.stream().map(EpicurvePoint::getLabelDate).toArray(LocalDate[]::new);
        this.caseDeltas = dataPoints.stream().map(EpicurvePoint::getCasesVm).toArray(Integer[]::new);
        this.deathDeltas = dataPoints.stream().map(EpicurvePoint::getDeathsVm).toArray(Integer[]::new);
        this.cases = dataPoints.stream().map(EpicurvePoint::getPositiveCount).toArray(Integer[]::new);
        this.deaths = dataPoints.stream().map(EpicurvePoint::getDeathCount).toArray(Integer[]::new);
        this.caseProjections = dataPoints.stream().map(EpicurvePoint::getCasesExtrapolated).toArray(Integer[]::new);
        this.movingAvgs = dataPoints.stream().map(EpicurvePoint::getMovingAvg).toArray(Integer[]::new);

        assertReport();
    }

    public void assertReport() {
        int dateCount = curveDates.length;

        Preconditions.checkArgument(dateCount == this.caseDeltas.length, "Count of caseDeltas does not match count of curveDates for report on " + this.reportDate);
        Preconditions.checkArgument(dateCount == this.deathDeltas.length, "Count of deathDeltas does not match count of curveDates for report on " + this.reportDate);
        Preconditions.checkArgument(dateCount == this.cases.length, "Count of cases does not match count of curveDates for report on " + this.reportDate);
        Preconditions.checkArgument(dateCount == this.deaths.length, "Count of deaths does not match count of curveDates for report on " + this.reportDate);
        Preconditions.checkArgument(dateCount == this.caseProjections.length, "Count of caseProjections does not match count of curveDates for report on " + this.reportDate);
        Preconditions.checkArgument(dateCount == this.movingAvgs.length, "Count of movingAvgs does not match count of curveDates for report on " + this.reportDate);
    }
}

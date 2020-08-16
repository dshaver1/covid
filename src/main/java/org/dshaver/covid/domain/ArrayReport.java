package org.dshaver.covid.domain;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.ToString;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Data
@ToString(exclude = {"curveDates", "caseDeltas", "deathDeltas", "cases", "deaths", "caseProjections", "movingAvgs"})
@Document("arrayreports")
public class ArrayReport {
    private static final LocalDate MIN_DATE = LocalDate.of(2020,2,17);
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
        this(report, "georgia");
    }

    public ArrayReport(Report report, String county) {
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

        // All of this is because sometimes the DPH publishes curves with missing dates.
        Collection<EpicurvePoint> dataPoints = report.getEpicurves().get(county.toLowerCase()).getData();
        Map<LocalDate, EpicurvePoint> dateMap = new HashMap<>();
        dataPoints.forEach(point -> dateMap.put(point.getLabelDate(), point));

        this.curveDates = Stream.iterate(MIN_DATE, d -> d.plusDays(1)).limit(ChronoUnit.DAYS.between(MIN_DATE, reportDate) + 1).toArray(LocalDate[]::new);
        this.cases = new Integer[curveDates.length];
        this.caseDeltas = new Integer[curveDates.length];
        this.deaths = new Integer[curveDates.length];
        this.deathDeltas = new Integer[curveDates.length];
        this.caseProjections = new Integer[curveDates.length];
        this.movingAvgs = new Integer[curveDates.length];

        EpicurvePoint currentPoint;
        LocalDate currentDate;
        for (int i = 0; i < curveDates.length; i++) {
            currentDate = curveDates[i];
            currentPoint = dateMap.get(currentDate);

            // Fill in 0's when we have a missing point.
            if (currentPoint == null) {
                this.cases[i] = 0;
                this.caseDeltas[i] = 0;
                this.deaths[i] = 0;
                this.deathDeltas[i] = 0;
                this.caseProjections[i] = 0;
                this.movingAvgs[i] = 0;
            } else {
                this.cases[i] = currentPoint.getPositiveCount() != null ? currentPoint.getPositiveCount() : 0;
                this.caseDeltas[i] = currentPoint.getCasesVm()!= null ? currentPoint.getCasesVm() : 0;
                this.deaths[i] = currentPoint.getDeathCount()!= null ? currentPoint.getDeathCount() : 0;
                this.deathDeltas[i] = currentPoint.getDeathsVm()!= null ? currentPoint.getDeathsVm() : 0;
                this.caseProjections[i] = currentPoint.getCasesExtrapolated()!= null ? currentPoint.getCasesExtrapolated() : 0;
                this.movingAvgs[i] = currentPoint.getMovingAvg()!= null ? currentPoint.getMovingAvg() : 0;
            }
        }

        assertReport();
    }

    private LocalDate[] extractContiguousDates(Collection<EpicurvePoint> dataPoints, LocalDate reportDate) {
        LocalDate min = dataPoints.stream().map(EpicurvePoint::getLabelDate).min(LocalDate::compareTo).orElse(LocalDate.of(2020,2,17));
        LocalDate max = dataPoints.stream().map(EpicurvePoint::getLabelDate).max(LocalDate::compareTo).orElse(reportDate);

        return Stream.iterate(min, d -> d.plusDays(1)).limit(ChronoUnit.DAYS.between(min, reportDate) + 1).toArray(LocalDate[]::new);
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

    public Integer[] getCaseDeltasNormalized() {
        return getReversedArray(getCaseDeltas(), 100);
    }

    public Integer[] getDeathDeltasNormalized() {
        return getReversedArray(getDeathDeltas(), 100);
    }

    private Integer[] getReversedArray(Integer[] array, int limit) {
        LinkedList<Integer> stack = new LinkedList<>();
        Arrays.stream(array).forEach(stack::push);

        Integer[] reversed = new Integer[limit];

        for (int i = 0; i < limit; i++) {
            if (i < stack.size()) {
                reversed[i] = stack.get(i);
            }
            else {
                reversed[i] = 0;
            }
        }

        return reversed;
    }
}

package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-22.
 */
@Data
@ToString(exclude = {"cumulativeCases", "casesPerDay", "cumulativeDeaths", "deathsPerDay"})
@Document("reports")
public class Report {
    private static final Logger logger = LoggerFactory.getLogger(Report.class);
    private static final String dataFolder = "H:\\dev\\covid\\data\\";
    private static final String cumulativeCasesFileName = "cumulativeCases_historic.csv";
    private static final String casesPerDayFileName = "casesPerDay_historic.csv";
    private static final String cumulativeDeathsFileName = "cumulativeDeaths_historic.csv";
    private static final String deathsPerDayFileName = "deathsPerDay_historic.csv";
    private static final String totalTestsPerformedFileName = "testsPerformed_historic.csv";

    @Id
    private String id;
    private LocalDateTime createTime;
    private LocalDate reportDate;
    private Collection<DataPoint> cumulativeCases;
    private Collection<DataPoint> casesPerDay;
    private Collection<DataPoint> cumulativeDeaths;
    private Collection<DataPoint> deathsPerDay;
    private int totalTests;
    private int confirmedCases;
    private int hospitalized;
    private int deaths;
    private Map<String, Integer> countyCaseMap;
    private Map<String, Integer> countyDeathMap;

    public Report(){}

    public Report(LocalDateTime createTime, String id, LocalDate reportDate, Series cumulativeCases, Series casesPerDay,
                  Series cumulativeDeaths, Series deathsPerDay, int totalTests, int confirmedCases, int hospitalized,
                  int deaths) {
        this.createTime = createTime;
        this.id = id;
        this.reportDate = reportDate;
        this.cumulativeCases = addSeries(cumulativeCases);
        this.casesPerDay = addSeries(casesPerDay);
        this.cumulativeDeaths = addSeries(cumulativeDeaths);
        this.deathsPerDay = addSeries(deathsPerDay);
        this.totalTests = totalTests;
        this.confirmedCases = confirmedCases;
        this.hospitalized = hospitalized;
        this.deaths = deaths;
    }

    public Collection<DataPoint> addSeries(Series series) {
        if (series == null || series.getDataPoints() == null || series.getDataPoints().isEmpty()) {
            return new ArrayList<>();
        }

        TreeSet<DataPoint> treeSet = new TreeSet<>(Comparator.comparing(DataPoint::getSource).thenComparing(DataPoint::getLabel));

        treeSet.addAll(series.getDataPoints());

        return treeSet;
    }

    public void writeReport() throws Exception {
        initializeFile(cumulativeCasesFileName, getCumulativeCases());
        initializeFile(casesPerDayFileName, getCasesPerDay());
        initializeFile(cumulativeDeathsFileName, getCumulativeDeaths());
        initializeFile(deathsPerDayFileName, getDeathsPerDay());
        initializeTestPerformedFile(totalTestsPerformedFileName);

        if (getCumulativeCases() != null) writeRow(cumulativeCasesFileName, getCumulativeCases(), getId());
        if (getCasesPerDay() != null) writeRow(casesPerDayFileName, getCasesPerDay(), getId());
        if (getCumulativeDeaths() != null) writeRow(cumulativeDeathsFileName, getCumulativeDeaths(), getId());
        if (getDeathsPerDay() != null) writeRow(deathsPerDayFileName, getDeathsPerDay(), getId());
        writeTestPerfomedRow(totalTestsPerformedFileName, getTotalTests(), getId());
    }

    private void writeTestPerfomedRow(String filename, int totalTests, String id) throws Exception {
        logger.info("Writing to file {}...", filename);
        String data = "\n" + id + "," + totalTests;
        Files.write(Paths.get(dataFolder, filename), data.getBytes(), StandardOpenOption.APPEND);
    }

    private void writeRow(String filename, Collection<DataPoint> series, String id) throws Exception {
        logger.info("Writing to file {}...", filename);
        String data = "\n" + id + "," + series.stream().map(point -> point.getY() + "").collect(Collectors.joining(","));
        Files.write(Paths.get(dataFolder, filename), data.getBytes(), StandardOpenOption.APPEND);
    }

    private void initializeTestPerformedFile(String filename) throws Exception{
        File file = new File(dataFolder + filename);
        if (!file.exists()) {
            String header = "RETRIEVAL_TIME,TOTAL_TESTS_PERFORMED";
            Files.write(Paths.get(dataFolder, filename), header.getBytes());
        }
    }

    private void initializeFile(String filename, Collection<DataPoint> series) throws Exception {
        File file = new File(dataFolder + filename);
        if (!file.exists()) {
            String header = series.stream().map(DataPoint::getLabel).collect(Collectors.joining(","));
            Files.write(Paths.get(dataFolder, filename), header.getBytes());
        }
    }

    public Integer getTotalDeaths() {
        if (getCumulativeDeaths() != null) {
            return getCumulativeDeaths().stream().mapToInt(DataPoint::getY).max().orElse(0);
        }

        return null;
    }

    public Integer getTotalCases() {
        if (getCumulativeCases() != null) {
            return getCumulativeCases().stream().mapToInt(DataPoint::getY).max().orElse(0);
        }

        return null;
    }
}

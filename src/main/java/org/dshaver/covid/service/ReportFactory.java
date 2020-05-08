package org.dshaver.covid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Component
public class ReportFactory {
    private static final Logger logger = LoggerFactory.getLogger(ReportFactory.class);
    private static final DateTimeFormatter SOURCE_LABEL_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter();
    private static final DateTimeFormatter SOURCE_LABEL_FORMAT_V2 = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ddMMMyyyy").toFormatter();
    private static final DateTimeFormatter TARGET_LABEL_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM-dd").toFormatter();
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);
    private static final Pattern epicurvePattern = Pattern.compile(".*JSON.parse\\('(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+EPICURVE\".+?}]}).*");
    private static final Pattern countyPattern = Pattern.compile(".*JSON.parse\\('(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+COUNTYCASES\".+?}]}).*");
    private static final Pattern overviewPattern = Pattern.compile(".*JSON.parse\\('(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+GA_COVID19_OVERALL\".+?}]}).*");
    private final Pattern onlyNumbersPattern = Pattern.compile("(\\d+).*");
    private final List<String> whiteList = new ArrayList<>();
    private final String filter = "\"dataPoints\" : ";
    private final ObjectMapper objectMapper;

    {
        whiteList.add("VAR ");
        whiteList.add("DATAPOINTS");
        whiteList.add("COMMERCIAL LAB");
        whiteList.add("GPHL");
        whiteList.add(">COVID-19 CONFIRMED CASES:");
        whiteList.add(">TOTAL");
        whiteList.add(">HOSPITALIZED");
        whiteList.add(">DEATHS");
    }

    @Inject
    public ReportFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Report createReport(RawDataV2 rawData) throws Exception {
        Optional<String> epicurveString = getVarFromRegex(rawData, epicurvePattern);
        Epicurve epicurve = null;
        if (epicurveString.isPresent()) {
            List<EpicurvePoint> filteredDataPoints = new ArrayList<>();
            epicurve = objectMapper.readValue(epicurveString.get(), Epicurve.class);
            for (EpicurvePoint current : epicurve.getEpicurvePoints()) {
                LocalDate labelDate = LocalDate.parse(current.getTestDate(), SOURCE_LABEL_FORMAT_V2);
                if (labelDate.isAfter(EARLIEST_DATE)) {
                    current.setSource(rawData.getId());
                    current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                    filteredDataPoints.add(current);
                }
            }
            epicurve.setEpicurvePoints(filteredDataPoints);
        }

        Optional<String> overviewString = getVarFromRegex(rawData, overviewPattern);
        ReportOverviewContainer reportOverviewContainer = null;
        if (overviewString.isPresent()) {
            reportOverviewContainer = objectMapper.readValue(overviewString.get(), ReportOverviewContainer.class);
        }

        // Just assuming there's only going to be one here... because why would there be more?
        ReportOverview overview = reportOverviewContainer.getReportOverviewList().get(0);

        Report report = new Report(LocalDateTime.now(),
                rawData.getId(),
                rawData.getReportDate(),
                epicurve,
                overview.getTotalTests(),
                overview.getConfirmedCovid(),
                overview.getHospitalization(),
                overview.getDeaths(),
                overview.getIcu());

        return report;
    }

    public Report createReport(RawDataV1 rawData) throws Exception {
        List<String> filteredStrings = rawData.getLines()
                .stream()
                .filter(s -> whiteList.stream().anyMatch(white -> s.toUpperCase().contains(white)))
                .collect(Collectors.toList());

        String ccasedayString = getVar("ccaseday", filteredStrings);
        //System.out.println(ccasedayString);
        String ccasecumString = getVar("ccasecum", filteredStrings);
        //System.out.println(ccasecumString);
        String cdeathdayString = getVar("cdeathday", filteredStrings);
        //System.out.println(cdeathdayString);
        String cdeathcumString = getVar("cdeathcum", filteredStrings);
        //System.out.println(cdeathcumString);

        Series ccasedaySeries = getSeries(ccasedayString, rawData.getId());
        Series ccasecumSeries = getSeries(ccasecumString, rawData.getId());
        Series cdeathdaySeries = getSeries(cdeathdayString, rawData.getId());
        Series cdeathcumSeries = getSeries(cdeathcumString, rawData.getId());

        int totalTestsPerformed = getTotalTests(filteredStrings);
        int confirmedCases = getTableValue(filteredStrings, "COVID-19 Confirmed Cases:", "Total");
        int hospitalized = getTableValue(filteredStrings, "COVID-19 Confirmed Cases:", "Hospitalized");
        int deaths = getTableValue(filteredStrings, "COVID-19 Confirmed Cases:", "Deaths");

        Epicurve epicurve = createEpicurveFromSeries(ccasedaySeries, ccasecumSeries, cdeathdaySeries, cdeathcumSeries);

        Report report = new Report(LocalDateTime.now(),
                rawData.getId(),
                rawData.getReportDate(),
                epicurve,
                totalTestsPerformed,
                confirmedCases,
                hospitalized,
                deaths);

        logger.info("Done parsing report for " + report.getId());

        return report;
    }

    private Optional<String> getVarFromRegex(RawDataV2 rawDataV2, Pattern pattern) {
        for (String s : rawDataV2.getPayload()) {
            Matcher epicurveMatcher = pattern.matcher(s);
            if (epicurveMatcher.matches()) {
                return Optional.of(epicurveMatcher.group(1));
            }
        }

        return Optional.empty();
    }

    private Epicurve createEpicurveFromSeries(Series caseDaySeries, Series caseCumSeries, Series deathDaySeries, Series deathCumSeries) {
        Epicurve epicurve = new Epicurve();
        epicurve.setExportFormat("Sourced from old DPH site");
        int minLength = caseDaySeries.getDataPoints().size();
        if (caseCumSeries.getDataPoints().size() < minLength) minLength = caseCumSeries.getDataPoints().size();
        if (deathDaySeries.getDataPoints().size() < minLength) minLength = deathDaySeries.getDataPoints().size();
        if (deathCumSeries.getDataPoints().size() < minLength) minLength = deathCumSeries.getDataPoints().size();

        // Only build the epicurve out to the point where we have all 4 series. Basically, we can't build the epicurve
        // if we don't have all 4 series for the date.
        for (int i = 0; i < minLength; i++) {
            EpicurvePoint epicurvePoint = new EpicurvePoint();
            StringBuilder labelBuilder = new StringBuilder();
            getReverseDatapoint(caseDaySeries, i).ifPresent(dataPoint -> {
                labelBuilder.append(dataPoint.getLabel());
                epicurvePoint.setLabel(dataPoint.getLabel());
                epicurvePoint.setSource(dataPoint.getSource());
                epicurvePoint.setTestDate(dataPoint.getLabel());
                epicurvePoint.setPositiveCount(dataPoint.getY());
            });
            String labelToMatch = labelBuilder.toString();

            getReverseDatapoint(caseCumSeries, i).ifPresent(dataPoint -> {
                if (!labelToMatch.equals(dataPoint.getLabel())) {
                    throw new IllegalArgumentException("caseCumSeries array label mismatch! Aborting report creation");
                }
                epicurvePoint.setPositivesCumulative(dataPoint.getY());
            });

            getReverseDatapoint(deathDaySeries, i).ifPresent(dataPoint -> {
                if (!labelToMatch.equals(dataPoint.getLabel())) {
                    throw new IllegalArgumentException("deathDaySeries array label mismatch! Aborting report creation");
                }
                epicurvePoint.setDeathCount(dataPoint.getY());
            });

            getReverseDatapoint(deathCumSeries, i).ifPresent(dataPoint -> {
                if (!labelToMatch.equals(dataPoint.getLabel())) {
                    throw new IllegalArgumentException("deathCumSeries array label mismatch! Aborting report creation");
                }
                epicurvePoint.setDeathsCumulative(dataPoint.getY());
            });

            epicurve.getEpicurvePoints().add(epicurvePoint);
        }

        return epicurve;
    }

    /**
     * Get the datapoint from the reverse end of the list. idx 0 will retrieve the last element. idx 1 will retrieve the
     * next to the last element, and so on until we get to idx {series.length - 1}, which will retrieve the first
     * element.
     */
    private Optional<DataPoint> getReverseDatapoint(Series series, int reverseIdx) {
        if (reverseIdx > series.getDataPoints().size()) {
            return Optional.empty();
        }

        return Optional.of(series.getDataPoints().get(series.getDataPoints().size() - 1 - reverseIdx));
    }

    private Series getSeries(String seriesString, String source) throws IOException {
        Series series = null;
        List<DataPoint> filteredDataPoints = new ArrayList<>();
        if (StringUtils.isNotEmpty(seriesString)) {
            series = objectMapper.readValue(seriesString, Series.class);
            if (series != null && series.getDataPoints() != null && !series.getDataPoints().isEmpty()) {
                for (DataPoint current : series.getDataPoints()) {
                    LocalDate labelDate = LocalDate.parse(current.getLabel(), SOURCE_LABEL_FORMAT);
                    if (labelDate.isAfter(EARLIEST_DATE)) {
                        current.setSource(source);
                        current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                        filteredDataPoints.add(current);
                    }
                }

                series.setDataPoints(filteredDataPoints);
            }
        }


        return series;
    }


    /**
     * Does the line contain any of my whitelist strings?
     */
    private boolean doICare(String line) {
        for (String white : whiteList) {
            if (line.contains(white)) {
                return true;
            }
        }

        return false;
    }


    private String getVar(String varName, List<String> downloadedStrings) {
        for (int i = 0; i < downloadedStrings.size(); i++) {
            String currentString = downloadedStrings.get(i);

            if (currentString.contains(varName)) {
                return parseJson(downloadedStrings.get(i + 1).trim());
            }
        }

        logger.error("Could not find var '{}'!", varName);

        return null;
    }

    private int getTotalTests(List<String> downloadedStrings) {
        int totalTests = 0;
        for (String testString : downloadedStrings) {
            if (testString.contains("Gphl") || testString.contains("Commercial Lab")) {
                String[] cellSplit = testString.split("</td><td class=\"tcell\">");
                String totalTestString = cellSplit[2].trim();
                Matcher matcher = onlyNumbersPattern.matcher(totalTestString);
                if (matcher.matches()) {
                    String totalTestStripped = matcher.group(1);
                    totalTests += Integer.parseInt(totalTestStripped);
                }
            }
        }
        System.out.println("Total Tests: " + totalTests);

        return totalTests;
    }

    private int getTableValue(List<String> downloadedStrings, String tableHeader, String valueKey) {
        boolean found = false;
        for (String testString : downloadedStrings) {
            if (found && testString.toUpperCase().contains(valueKey.toUpperCase())) {
                String[] cellSplit = testString.split("</td><td class=\"tcell\">");
                String totalTestString = cellSplit[1].trim();
                Matcher matcher = onlyNumbersPattern.matcher(totalTestString);
                if (matcher.matches()) {
                    String totalTestStripped = matcher.group(1);
                    System.out.println(valueKey + ": " + totalTestStripped);
                    return Integer.parseInt(totalTestStripped);
                }
            }

            if (testString.toUpperCase().contains(tableHeader.toUpperCase())) {
                found = true;
            }
        }

        return 0;
    }

    private String parseJson(String json) {
        // cut off the last bracket
        String filtered = json.substring(0, json.length() - 1);
        filtered = "{" + filtered;

        return filtered;

    }
}

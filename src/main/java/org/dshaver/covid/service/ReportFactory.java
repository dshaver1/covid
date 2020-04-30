package org.dshaver.covid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.Series;
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
    private static final DateTimeFormatter TARGET_LABEL_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM-dd").toFormatter();
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

    public Report createReport(RawData rawData) throws Exception {
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

        Report report = new Report(LocalDateTime.now(),
                rawData.getId(),
                rawData.getReportDate(),
                ccasecumSeries,
                ccasedaySeries,
                cdeathcumSeries,
                cdeathdaySeries,
                totalTestsPerformed,
                confirmedCases,
                hospitalized,
                deaths);

        logger.info("Done parsing report for " + report.getId());

        return report;
    }

    private Series getSeries(String seriesString, String source) throws IOException {
        Series series = null;
        if (StringUtils.isNotEmpty(seriesString)) {
            series = objectMapper.readValue(seriesString, Series.class);
            series.getDataPoints().forEach(d -> {
                d.setSource(source);
                d.setLabel(LocalDate.parse(d.getLabel(), SOURCE_LABEL_FORMAT).format(DateTimeFormatter.ISO_DATE).toUpperCase());
            });
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

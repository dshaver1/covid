package org.dshaver.covid.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by xpdf64 on 2020-04-29.
 */
@Data
public class AggregateReport {
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-uu");
    private NavigableSet<DataPoint> totalDeaths;
    private NavigableSet<DataPoint> totalCases;
    private NavigableSet<DataPoint> totalTests;
    private NavigableSet<DataPoint> newDeaths;
    private NavigableSet<DataPoint> newCases;
    private NavigableSet<DataPoint> newTests;
    private LocalDate minDate;
    private LocalDate maxDate;

    public AggregateReport(List<Report> reportList) {
        Map<String, DataPoint> tempTotalDeathMap = new HashMap<>();
        Map<String, DataPoint> tempTotalCaseMap = new HashMap<>();
        Map<String, DataPoint> tempTotalTestMap = new HashMap<>();
        totalDeaths = new TreeSet<>(Comparator.comparing(DataPoint::getSource));
        totalCases = new TreeSet<>(Comparator.comparing(DataPoint::getSource));
        totalTests = new TreeSet<>(Comparator.comparing(DataPoint::getSource));
        minDate = reportList.get(0).getReportDate();
        maxDate = reportList.get(0).getReportDate();

        for (Report report : reportList) {
            if (report.getReportDate().isBefore(minDate)) {
                minDate = report.getReportDate();
            }

            if (report.getReportDate().isAfter(maxDate)) {
                maxDate = report.getReportDate();
            }

            if (report.getTotalDeaths() != null) {
                tempTotalDeathMap.put(report.getReportDate().toString(), new DataPoint(report.getTotalDeaths(), report.getReportDate().format(LABEL_FORMAT).toUpperCase(), report.getId()));
            }

            if (report.getTotalCases() != null) {
                tempTotalCaseMap.put(report.getReportDate().toString(), new DataPoint(report.getTotalCases(), report.getReportDate().format(LABEL_FORMAT).toUpperCase(), report.getId()));
            }

            tempTotalTestMap.put(report.getReportDate().toString(), new DataPoint(report.getTotalTests(), report.getReportDate().format(LABEL_FORMAT).toUpperCase(), report.getId()));
        }

        totalDeaths.addAll(tempTotalDeathMap.values());
        totalCases.addAll(tempTotalCaseMap.values());
        totalTests.addAll(tempTotalTestMap.values());

        newDeaths = calculateReturns(totalDeaths);
        newCases = calculateReturns(totalCases);
        newTests = calculateReturns(totalTests);
    }

    private TreeSet<DataPoint> calculateReturns(Collection<DataPoint> dataPoints) {
        TreeSet<DataPoint> returns = new TreeSet<>(Comparator.comparing(DataPoint::getSource));

        DataPoint previous = null;
        for (DataPoint current : dataPoints) {
            if (previous != null && previous.getY() != 0) {
                int returnValue = current.getY() - previous.getY();
                returns.add(current.copy(returnValue));
            }

            previous = current;
        }

        return returns;
    }
}

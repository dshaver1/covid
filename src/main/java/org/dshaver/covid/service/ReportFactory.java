package org.dshaver.covid.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.dao.*;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.domain.epicurve.*;
import org.dshaver.covid.domain.overview.ReportOverview;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl1;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.dshaver.covid.service.extractor.Extractor;
import org.dshaver.covid.service.extractor.HealthcareWorkerExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.dshaver.covid.service.CsvService.cleanCounty;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Component
public class ReportFactory {
    private static final Logger logger = LoggerFactory.getLogger(ReportFactory.class);
    private static final DateTimeFormatter SOURCE_LABEL_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter();
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);
    private static final LocalDate HEALTHCARE_BEGIN_DATE = LocalDate.of(2020, 6, 1);
    private static final LocalDate EXTRACTOR_2_BEGIN_DATE = LocalDate.of(2020, 5, 11);
    private final Pattern onlyNumbersPattern = Pattern.compile("(\\d+).*");
    private final List<String> whiteList = new ArrayList<>();
    private final String filter = "\"dataPoints\" : ";
    private final ObjectMapper objectMapper;
    private final EpicurveDtoV2Repository epicurveDtoV2Repository;
    private final HealthcareDtoRepository healthcareDtoRepository;
    private final TestingStatsRepository testingStatsRepository;
    private final Extractor<String, ReportOverview> reportOverviewExtractor;
    private final ReportOverviewRepositoryV1 reportOverviewRepositoryV1;
    private final EpicurveExtractorImpl1 epicurveExtractor1;
    private final EpicurveExtractorImpl2 epicurveExtractor2;
    private final HealthcareWorkerExtractor healthcareWorkerExtractor;
    private final CountyOverviewRepository countyOverviewRepository;

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
    public ReportFactory(ObjectMapper objectMapper,
                         EpicurveDtoV2Repository epicurveDtoV2Repository,
                         HealthcareDtoRepository healthcareDtoRepository,
                         TestingStatsRepository testingStatsRepository,
                         @Qualifier("reportOverviewExtractorDelegator") Extractor<String, ReportOverview> reportOverviewExtractor,
                         ReportOverviewRepositoryV1 reportOverviewRepositoryV1,
                         EpicurveExtractorImpl1 epicurveExtractor1,
                         EpicurveExtractorImpl2 epicurveExtractor2,
                         HealthcareWorkerExtractor healthcareWorkerExtractor,
                         CountyOverviewRepository countyOverviewRepository) {
        this.objectMapper = objectMapper;
        this.epicurveDtoV2Repository = epicurveDtoV2Repository;
        this.healthcareDtoRepository = healthcareDtoRepository;
        this.testingStatsRepository = testingStatsRepository;
        this.reportOverviewExtractor = reportOverviewExtractor;
        this.reportOverviewRepositoryV1 = reportOverviewRepositoryV1;
        this.epicurveExtractor1 = epicurveExtractor1;
        this.epicurveExtractor2 = epicurveExtractor2;
        this.healthcareWorkerExtractor = healthcareWorkerExtractor;
        this.countyOverviewRepository = countyOverviewRepository;
    }

    public Report createReport(RawData rawData, Report previousReport) throws Exception {
        if (rawData instanceof RawDataV2) {
            logger.info("About to start creating report from RawDataV2");
            return createReport(rawData.getId(), rawData.getReportDate(), previousReport);
        }

        if (rawData instanceof RawDataV1) {
            logger.info("About to start creating report from RawDataV1");
            return createReport(rawData.getId(), rawData.getReportDate(), previousReport);
        }

        if (rawData instanceof RawDataV0) {
            logger.info("About to start creating report from RawDataV0");
            return createReport((RawDataV0) rawData, previousReport);
        }

        if (rawData instanceof ManualRawData) {
            logger.info("About to start creating report from ManualRawData");
            return createReport((ManualRawData) rawData, previousReport);
        }

        return null;
    }

    public Report createReport(ManualRawData rawData, Report previousReport) throws Exception {
        List<EpicurvePoint> epicurvePoints =
                objectMapper.readValue(rawData.getPayload().get(0), new TypeReference<List<EpicurvePointImpl2>>() {
                });
        Epicurve epicurve = new Epicurve("Georgia");
        epicurve.setData(epicurvePoints);

        Map<String, Epicurve> epicurves = new HashMap<>();
        epicurves.put("Georgia", epicurve);

        Report report = new Report(LocalDateTime.now(),
                rawData.getId(),
                rawData.getReportDate(),
                epicurves,
                new HashMap<>(),
                rawData.getTotalTests(),
                rawData.getConfirmedCases(),
                rawData.getHospitalizations(),
                rawData.getDeaths(),
                rawData.getIcu(),
                previousReport == null ? 0 : rawData.getTotalTests() - previousReport.getTotalTests(),
                previousReport == null ? 0 : rawData.getConfirmedCases() - previousReport.getConfirmedCases(),
                previousReport == null ? 0 : rawData.getHospitalizations() - previousReport.getHospitalized(),
                previousReport == null ? 0 : rawData.getDeaths() - previousReport.getDeaths(),
                previousReport == null ? 0 : rawData.getIcu() - previousReport.getIcu());

        // Calculate VM
        VmCalculator.populateVm(report, previousReport);

        // Calculate biggest deltas
        report.setTop5CaseDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getCasesVm));
        report.setTop5DeathDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getDeathsVm));

        logger.info("Done parsing report for " + report.getId());

        return report;
    }

    public Report createReport(String id, LocalDate reportDate, Report previousReport) {
        Optional<EpicurvePointImpl2Container> epicurveContainer = epicurveDtoV2Repository.findById(id);
        Optional<HealthcareWorkerEpiPointContainer> healthcareContainer = reportDate.isAfter(HEALTHCARE_BEGIN_DATE) ? healthcareDtoRepository.findById(id) : Optional.empty();
        Optional<CountyOverviewContainer> countyOverviewContainer = countyOverviewRepository.findById(id);

        if (!epicurveContainer.isPresent()) {
            throw new IllegalStateException("Could not find main epicurve within raw data!");
        }

        if (!healthcareContainer.isPresent()) {
            logger.info("Could not find healthcare epicurve within raw data!");
        }

        Optional<Map<String, Epicurve>> maybeEpicurve = epicurveExtractor2.extract(epicurveContainer.get().getPayload(), id);
        Optional<Map<String, Epicurve>> maybeHealthcareEpicurve = Optional.empty();
        if (healthcareContainer.isPresent()) {
            maybeHealthcareEpicurve = healthcareWorkerExtractor.extract(healthcareContainer.get().getPayload(), id);
        }

        if (!maybeEpicurve.isPresent()) {
            throw new IllegalStateException("Could not extract epicurve from !");
        }

        Optional<ReportOverviewImpl2> maybeOverview = reportOverviewRepositoryV1.findById(id);
        //Optional<ReportOverview> maybeOverview = reportOverviewExtractor.extract(rawData.getPayload(), rawData.getId());

        if (!maybeOverview.isPresent()) {
            throw new IllegalStateException("Could not find ReportOverview within raw data!");
        }

        Map<String, Epicurve> epicurves = maybeEpicurve.get();

        if (maybeHealthcareEpicurve.isPresent()) {
            logger.info("Found healthcare epicurve!");
            epicurves.put("healthcare", maybeHealthcareEpicurve.get().get("healthcare"));
        }

        Map<String, CountyOverview> countyOverviews = new HashMap<>();
        countyOverviewContainer.ifPresent(overviewContainer -> overviewContainer.getPayload().forEach(o -> {
            String cleanCounty = cleanCounty(o.getCountyName());
            CountyOverview previousCountyOverview = previousReport.getCountyOverviewMap().get(cleanCounty);
            int positiveVm = 0;
            int deathsVm = 0;
            if (previousCountyOverview != null) {
                positiveVm = o.getPositive() - (previousReport.getCountyOverviewMap().get(cleanCounty).getPositive());
                deathsVm = o.getDeaths() - (previousReport.getCountyOverviewMap().get(cleanCounty).getDeaths());
            }
            o.setPositiveVm(positiveVm);
            o.setDeathsVm(deathsVm);

            countyOverviews.put(o.getCountyName().toLowerCase(), o);
        }));

        ReportOverview overview = maybeOverview.get();
        Report report = new Report(LocalDateTime.now(),
                id,
                reportDate,
                epicurves,
                countyOverviews,
                overview.getTotalTests(),
                overview.getConfirmedCovid(),
                overview.getHospitalization(),
                overview.getDeaths(),
                overview.getIcu(),
                previousReport == null ? 0 : overview.getTotalTests() - previousReport.getTotalTests(),
                previousReport == null ? 0 : overview.getConfirmedCovid() - previousReport.getConfirmedCases(),
                previousReport == null ? 0 : overview.getHospitalization() - previousReport.getHospitalized(),
                previousReport == null ? 0 : overview.getDeaths() - previousReport.getDeaths(),
                previousReport == null ? 0 : overview.getIcu() - previousReport.getIcu());

        // Calculate VM
        VmCalculator.populateVm(report, previousReport);

        // Calculate biggest deltas
        report.setTop5CaseDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getCasesVm));
        report.setTop5DeathDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getDeathsVm));

        logger.info("Done parsing report for " + report.getId());

        return report;
    }

    public Report createReport(RawDataV0 rawData, Report previousReport) throws Exception {
        List<String> filteredStrings = rawData.getPayload()
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

        // Get DTO
        EpicurveDtoImpl1 epicurveDto = createEpicurveFromSeries(ccasedaySeries, ccasecumSeries, cdeathdaySeries, cdeathcumSeries);

        // Convert to target epicurve object
        Map<String, Epicurve> epicurves = new HashMap<>();
        epicurveDto.getAllEpicurves().asMap().forEach((county, points) -> {
            Epicurve countyEpicurve = epicurves.computeIfAbsent(county.toLowerCase(), k -> new Epicurve(county));
            countyEpicurve.setData(points);
        });

        Report report = new Report(LocalDateTime.now(),
                rawData.getId(),
                rawData.getReportDate(),
                epicurves,
                totalTestsPerformed,
                confirmedCases,
                hospitalized,
                deaths,
                previousReport == null ? 0 : totalTestsPerformed - previousReport.getTotalTests(),
                previousReport == null ? 0 : confirmedCases - previousReport.getConfirmedCases(),
                previousReport == null ? 0 : hospitalized - previousReport.getHospitalized(),
                previousReport == null ? 0 : deaths - previousReport.getDeaths());

        // Calculate VM
        VmCalculator.populateVm(report, previousReport);

        // Calculate biggest deltas
        report.setTop5CaseDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getCasesVm));
        report.setTop5DeathDeltas(VmCalculator.calculateTopDeltas(report, EpicurvePoint::getDeathsVm));

        logger.info("Done parsing report for " + report.getId());

        return report;
    }

    private EpicurveDtoImpl1 createEpicurveFromSeries(Series caseDaySeries, Series caseCumSeries, Series deathDaySeries, Series deathCumSeries) {
        EpicurveDtoImpl1 epicurve = new EpicurveDtoImpl1();
        epicurve.setExportFormat("Sourced from old DPH site");
        int minLength = caseDaySeries.getDataPoints().size();
        if (caseCumSeries.getDataPoints().size() < minLength) minLength = caseCumSeries.getDataPoints().size();
        if (deathDaySeries.getDataPoints().size() < minLength) minLength = deathDaySeries.getDataPoints().size();
        if (deathCumSeries.getDataPoints().size() < minLength) minLength = deathCumSeries.getDataPoints().size();

        // Only build the epicurve out to the point where we have all 4 series. Basically, we can't build the epicurve
        // if we don't have all 4 series for the date.
        for (int i = 0; i < minLength; i++) {
            EpicurvePointImpl1 epicurvePoint = new EpicurvePointImpl1();
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


    private String getVar(String varName, List<String> splitStrings) {
        for (int i = 0; i < splitStrings.size(); i++) {
            String currentString = splitStrings.get(i);

            if (currentString.contains(varName)) {
                return parseJson(splitStrings.get(i + 1).trim());
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

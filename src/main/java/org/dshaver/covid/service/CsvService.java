package org.dshaver.covid.service;

import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.domain.ArrayReport;
import org.dshaver.covid.domain.HistogramReportContainer;
import org.dshaver.covid.domain.HistogramReportV2;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvService {
    private static final Logger logger = LoggerFactory.getLogger(CsvService.class);
    private static final String[] SUMMARY_HEADER = new String[]{"id", "createTime", "reportDate", "totalTests", "totalTestVm",
            "totalConfirmedCases", "totalDeaths", "confirmedCasesVm", "hospitalized", "hospitalizedVm", "deathsVm",
            "icu", "icuVm", "casesVsDeathsCorrelation"};

    private final CountyService countyService;

    @Inject
    public CsvService(CountyService countyService) {
        this.countyService = countyService;
    }

    public String[] createHeader(Collection<ArrayReport> reports) {
        List<String> header = new ArrayList<>();
        header.add("id");
        header.addAll(Arrays.stream(reports.stream()
                .skip(reports.size() - 1)
                .findFirst()
                .get()
                .getCurveDates())
                .map(LocalDate::toString)
                .collect(Collectors.toList()));

        return header.toArray(new String[]{});
    }

    public String[] createHeader(Report report) {
        List<String> headerList = new ArrayList<>();
        headerList.add("id");
        String[] header = new String[0];
        try {
            headerList.addAll(Arrays.stream(new ArrayReport(report).getCurveDates())
                    .map(LocalDate::toString)
                    .collect(Collectors.toList()));

            header = headerList.toArray(new String[]{});

        } catch (Exception e) {
            logger.error("Could not extract header from report with id " + report.getId(), e);
        }

        return header;
    }

    public String writeSummary(Path path, Collection<ArrayReport> reports) {
        List<String> result = new ArrayList<>();

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.error("Error deleting old summary csv " + path);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            String[] header = new String[]{"id", "createTime", "reportDate", "totalTests", "totalTestVm",
                    "totalConfirmedCases", "totalDeaths", "confirmedCasesVm", "hospitalized", "hospitalizedVm", "deathsVm",
                    "icu", "icuVm", "casesVsDeathsCorrelation"};

            writer.write(String.join(",", header));

            List<String> row = new ArrayList<>();
            for (ArrayReport report : reports) {
                writer.write("\n");

                row.add(report.getId());
                row.add(report.getCreateTime().toString());
                row.add(report.getReportDate().toString());
                row.add("" + report.getTotalTests());
                row.add("" + report.getTotalTestsVm());
                row.add("" + report.getTotalConfirmedCases());
                row.add("" + report.getTotalDeaths());
                row.add("" + report.getConfirmedCasesVm());
                row.add("" + report.getHospitalized());
                row.add("" + report.getHospitalizedVm());
                row.add("" + report.getDeathsVm());
                row.add("" + report.getIcu());
                row.add("" + report.getIcuVm());

                writer.write(String.join(",", row));

                writer.flush();

                row.clear();
            }
        } catch (IOException e) {
            logger.error("Error writing summary csv: " + path, e);
        }

        try {
            result = Files.readAllLines(path);
        } catch (IOException e) {
            logger.error("Error reading back summary file: " + path, e);
        }

        return String.join("\n", result);
    }

    public String readFile(String dir, String filename) throws Exception {
        Path path = Paths.get(dir).resolve(filename);
        return String.join("\n", Files.readAllLines(path));
    }

    public void deleteAllCsvs(String dir) {
        logger.info("Cleaning up ALL existing csvs...");

        for (String county : countyService.getAllEnabledCounties()) {
            logger.info("Cleaning up csvs for {}!", county);
            try {
                Files.list(getCountyDirPath(dir, county)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.info("Could not delete {} because it doesn't exist!", path);
                    }
                });
            } catch (NoSuchFileException e) {
                logger.info("Could not delete files in {} because it doesn't exist!", dir);
            } catch (IOException e) {
                logger.error("Error deleting directory contents for " + county, e);
            }
        }
    }

    public Path getCountyDirPath(String baseDir, String county) {
        return Paths.get(baseDir, county);
    }

    public Path getCountyFilePath(String dir, String type, String county) {
        Path path = Paths.get(dir).resolve(String.format("%s.csv", type));
        if (county != null) {
            String filteredCounty = cleanCounty(county);

            path = getCountyDirPath(dir, filteredCounty).resolve(String.format("%s_%s.csv", type, filteredCounty));
        }

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            logger.error("Error creating county csv path!", e);
        }

        return path;
    }

    public void delete(Path path, String county) {
        try {
            Files.deleteIfExists(path.resolve(String.format("cases_%s.csv", county)));
            Files.deleteIfExists(path.resolve(String.format("caseDeltas_%s.csv", county)));
            Files.deleteIfExists(path.resolve(String.format("movingAvgs_%s.csv", county)));
            Files.deleteIfExists(path.resolve(String.format("deaths_%s.csv", county)));
            Files.deleteIfExists(path.resolve(String.format("deathDeltas_%s.csv", county)));
        } catch (IOException e) {
            logger.error("Could not delete files!", e);
        }
    }
    public void appendHistogramFile(Path path, String county, String[] header, HistogramReportContainer report, Function<HistogramReportV2, BigDecimal[]> bdFunction) {
        HistogramReportV2 countyReport = report.getCountyHistogramMap().get(county);

        try {
            BufferedWriter writer = null;
            if (path.toFile().exists()) {
                writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);
                // Check if we need to do anything...
                try (FileReader fileReader = new FileReader(path.toFile()); BufferedReader br = new BufferedReader(fileReader)) {
                    String currentString;
                    String lastDateString = "";
                    while ((currentString = br.readLine()) != null) {
                        lastDateString = currentString.split(",")[0];
                    }

                    if (report.getReportDate().toString().equals(lastDateString)) {
                        logger.debug("Thought we needed to append to the csv {}, but actually didn't.", path);
                        return;
                    }
                }
            } else {
                writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
                writer.write(String.join(",", header));
                writer.write("\n");
            }

            List<String> row = new ArrayList<>();

            row.add(report.getReportDate().toString());
            row.addAll(Arrays.stream(bdFunction.apply(countyReport)).map(Object::toString).collect(Collectors.toList()));

            writer.write(String.join(",", row));

            writer.write("\n");

            writer.close();
        } catch (IOException e) {
            logger.error("Error writing histogram report! Report date: " + report.getReportDate(), e);
        }
    }

    public void appendFile(Path path, String county, String[] header, Report report, Function<ArrayReport, Integer[]> intFunction) throws Exception {
        ArrayReport arrayReport = new ArrayReport(report, county);
        List<String> existingRows = new ArrayList<>();

        BufferedWriter writer = getBufferedWriter(path, header, existingRows);
        if (writer == null) return;

        List<String> row = new ArrayList<>();

        row.add(report.getId());
        row.addAll(Arrays.stream(intFunction.apply(arrayReport)).map(Object::toString).collect(Collectors.toList()));

        writer.write(String.join(",", row));

        writer.close();
    }

    private BufferedWriter getBufferedWriter(Path path, String[] header, List<String> existingRows) throws IOException {
        BufferedWriter writer = null;
        if (path.toFile().exists()) {
            // Check to see if we even need to append anything.
            try (FileReader fileReader = new FileReader(path.toFile()); BufferedReader br = new BufferedReader(fileReader)) {
                // Read existing header
                String existingHeader = br.readLine();
                if (StringUtils.isNotEmpty(existingHeader)) {
                    String[] existingHeaderArray = existingHeader.split(",");

                    // Only append if the new header is exactly 1 longer than existing header... otherwise just return without doing anything.
                    if (header.length - existingHeaderArray.length != 1) {
                        logger.debug("Thought we needed to append to the csv {}, but actually didn't.", path);
                        return null;
                    }

                    String currentString;
                    // Save the rest of the file to memory so we can prepend the new header.
                    while ((currentString = br.readLine()) != null) {
                        existingRows.add(currentString);
                    }

                    writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
                    // Rewrite header
                    writer.write(String.join(",", header));

                    // Add back the rest of the existing file
                    for (String currentRow : existingRows) {
                        writer.write("\n");
                        writer.write(currentRow);
                    }
                }
            }

        } else {
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
            writer.write(String.join(",", header));
        }

        // Now write the new line
        writer.write("\n");
        return writer;
    }

    public void appendSummary(Path path, String county, Report report) throws Exception {
        ArrayReport arrayReport = new ArrayReport(report, county);

        BufferedWriter writer = null;
        if (path.toFile().exists()) {
            writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);

            // Check to see if we even need to append anything
            String currentLine = "";
            try (FileReader fileReader = new FileReader(path.toFile()); BufferedReader br = new BufferedReader(fileReader)) {
                while ((currentLine = br.readLine()) != null) {
                    if (currentLine.contains(report.getId())) {
                        logger.info("Thought we needed to append to the csv {}, but actually didn't. {} contains {}.", path, currentLine, report.getId());
                        return;
                    }
                }
            }

        } else {
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
            writer.write(String.join(",", SUMMARY_HEADER));
        }

        try {
            List<String> row = new ArrayList<>();
            writer.write("\n");

            row.add(arrayReport.getId());
            row.add(arrayReport.getCreateTime().toString());
            row.add(arrayReport.getReportDate().toString());
            row.add("" + arrayReport.getTotalTests());
            row.add("" + arrayReport.getTotalTestsVm());
            row.add("" + arrayReport.getTotalConfirmedCases());
            row.add("" + arrayReport.getTotalDeaths());
            row.add("" + arrayReport.getConfirmedCasesVm());
            row.add("" + arrayReport.getHospitalized());
            row.add("" + arrayReport.getHospitalizedVm());
            row.add("" + arrayReport.getDeathsVm());
            row.add("" + arrayReport.getIcu());
            row.add("" + arrayReport.getIcuVm());

            writer.write(String.join(",", row));

            writer.flush();

            row.clear();

        } catch (IOException e) {
            logger.error("Error writing summary csv: " + path, e);
        } finally {
            writer.close();
        }

        try {
            Files.readAllLines(path);
        } catch (IOException e) {
            logger.error("Error reading back summary file: " + path, e);
        }
    }

    public void updateHeader(Path path, String[] header) throws Exception {
        List<String> existingRows = new ArrayList<>();

        Files.createDirectories(path.getParent());

        try (FileReader fileReader = new FileReader(path.toFile()); BufferedReader br = new BufferedReader(fileReader)) {
            // Skip existing header
            br.readLine();
            String currentString;
            // Save the rest of the file to memory so we can prepend the new header.
            while ((currentString = br.readLine()) != null) {
                existingRows.add(currentString);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            writer.write(String.join(",", header));

            for (String currentRow : existingRows) {
                writer.write("\n");
                writer.write(currentRow);
            }
        }
    }

    public String writeFile(String dir, String filename, String[] header, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) throws Exception {
        Path path = Paths.get(dir).resolve(filename);
        Files.deleteIfExists(path);
        BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);

        writer.write(String.join(",", header));

        writeLines(writer, reports, intFunction);

        writer.close();

        return String.join("\n", Files.readAllLines(path));
    }

    public void writeLines(BufferedWriter writer, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) throws IOException {
        List<String> row = new ArrayList<>();

        for (ArrayReport report : reports) {
            writer.write("\n");

            row.add(report.getId());
            row.addAll(Arrays.stream(intFunction.apply(report)).map(Object::toString).collect(Collectors.toList()));

            writer.write(String.join(",", row));

            row = new ArrayList<>();
        }
    }

    public static String cleanCounty(String originalCounty) {
        String filteredCounty = originalCounty.toLowerCase().replace(" ", "-");
        filteredCounty = filteredCounty.replace("/", "");
        filteredCounty = filteredCounty.replace("\\", "");
        filteredCounty = filteredCounty.replace("unknown-state", "");
        filteredCounty = filteredCounty.replace("non-ga-resident", "non-georgia-resident");
        filteredCounty = filteredCounty.replace("non-georgiaresident", "non-georgia-resident");
        filteredCounty = filteredCounty.replace("non-garesidentunknownstate", "non-georgia-resident");
        filteredCounty = filteredCounty.replace("dekalbg", "dekalb");

        return filteredCounty;
    }
}

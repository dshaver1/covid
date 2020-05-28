package org.dshaver.covid.service;

import com.opencsv.CSVWriter;
import org.dshaver.covid.domain.ArrayReport;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvService {

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

    public String writeSummary(String dir, String filename, Collection<ArrayReport> reports) throws Exception {
        Path path = Paths.get(dir).resolve(filename);
        CSVWriter writer =
                new CSVWriter(Files.newBufferedWriter(path, StandardOpenOption.CREATE));

        String[] header = new String[]{"id", "createTime", "reportDate", "totalTests", "totalTestVm",
                "totalConfirmedCases", "totalDeaths", "confirmedCasesVm", "hospitalized", "hospitalizedVm", "deathsVm",
                "icu", "icuVm"};

        writer.writeNext(header);

        List<String> row = new ArrayList<>();
        for (ArrayReport report : reports) {
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

            writer.writeNext(row.toArray(new String[]{}));

            row = new ArrayList<>();
        }

        writer.close();

        return String.join("\n", Files.readAllLines(path));
    }

    public String readFile(String dir, String filename) throws Exception {
        Path path = Paths.get(dir).resolve(filename);
        return String.join("\n", Files.readAllLines(path));
    }

    public String writeFile(String dir, String filename, String[] header, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) throws Exception {
        Path path = Paths.get(dir).resolve(filename);
        CSVWriter writer =
                new CSVWriter(Files.newBufferedWriter(path, StandardOpenOption.CREATE));

        writer.writeNext(header);

        writeLines(writer, reports, intFunction);

        writer.close();

        return String.join("\n", Files.readAllLines(path));
    }

    public void writeLines(CSVWriter writer, Collection<ArrayReport> reports, Function<ArrayReport, Integer[]> intFunction) {
        List<String> row = new ArrayList<>();

        for (ArrayReport report : reports) {
            row.add(report.getId());
            row.addAll(Arrays.stream(intFunction.apply(report)).map(Object::toString).collect(Collectors.toList()));

            writer.writeNext(row.toArray(new String[]{}));

            row = new ArrayList<>();
        }
    }
}

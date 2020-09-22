package org.dshaver.covid.domain;

import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

@Data
public class HistogramReportContainer implements Identifiable {
    String id;
    LocalDate reportDate;
    Integer windowSize;
    Path filePath;
    Map<String, HistogramReportV2> countyHistogramMap;
}

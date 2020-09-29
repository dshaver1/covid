package org.dshaver.covid.domain;

import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Created by xpdf64 on 2020-04-29.
 */
@Data
public class AggregateReport implements Identifiable {
    String id;
    LocalDate reportDate;
    Path filePath;
    /**
     * List of counties ordered by the number of days it takes to reach 90% cumulative cases reported.
     */
    Collection<CountyValuePair> daysTo90PercentCases;

    /**
     * List of points that breach the max seen in the last 28 days for that 0-baselined day.
     */
    Collection<EpicurvePoint> breaches;
}

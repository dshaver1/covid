package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class HealthcareWorkerExtractor extends AbstractExtractor implements Extractor<HealthcareWorkerEpiPoint, Map<String, Epicurve>> {
    private static final Logger logger = LoggerFactory.getLogger(HealthcareWorkerExtractor.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\[\\{\"test_date\":\"\\d{4}-\\d{2}-\\d{2}\",\"ethnicity\".+?}])");
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);

    @Inject
    protected HealthcareWorkerExtractor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<Map<String, Epicurve>> extract(List<HealthcareWorkerEpiPoint> raw, String id) {
        logger.info("Extracting healthcare epicurve data from " + id);
        List<EpicurvePoint> epicurvePoints = null;
        Optional<Map<String, Epicurve>> epicurve = Optional.empty();
        Map<LocalDate, Integer> caseTotals = new HashMap<>();
        Map<LocalDate, Integer> deathTotals = new HashMap<>();

        try {
            Map<String, Collection<EpicurvePoint>> filteredDataPoints = new HashMap<>();
            Map<String, List<EpicurvePoint>> groupedByDate = raw.stream().collect(Collectors.groupingBy(EpicurvePoint::getTestDate));
            epicurvePoints = groupedByDate.values().stream().map(list -> list.stream().reduce(new HealthcareWorkerEpiPoint("healthcare"), (o1, o2) -> {
                o1.setTestDate(o2.getTestDate());
                o1.setLabel(o2.getTestDate());
                o1.setPositiveCount(o1.getPositiveCount() + o2.getPositiveCount());
                o1.setDeathCount(o1.getDeathCount() + o2.getDeathCount());

                return o1;
            })).collect(Collectors.toList());
            for (EpicurvePoint current : epicurvePoints) {
                LocalDate labelDate = LocalDate.parse(current.getTestDate(), DateTimeFormatter.ISO_DATE);
                // Next iterate over the points and filter/decorate as needed.
                if (labelDate.isAfter(EARLIEST_DATE)) {
                    current.setSource(id);
                    current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                    current.setLabelDate(labelDate);
                    Collection<EpicurvePoint> countyPoints = filteredDataPoints.computeIfAbsent(current.getCounty().toLowerCase(), k -> new TreeSet<>());
                    countyPoints.add(current);

                    // Add to totals. We're doing this to QA the totals DPH is reporting.
                    if (!current.getCounty().toLowerCase().equals("georgia")) {
                        caseTotals.merge(labelDate, current.getPositiveCount(), Integer::sum);
                        deathTotals.merge(labelDate, current.getDeathCount(), Integer::sum);
                    }
                }
            }

            Map<String, Epicurve> countyToCurveMap = new HashMap<>();

            // Now we construct our target object
            filteredDataPoints.forEach((county, points) -> {
                Epicurve countyEpicurve = countyToCurveMap.computeIfAbsent(county.toLowerCase(), k -> new Epicurve(county));
                countyEpicurve.setData(points);
            });

            // And put it in an optional to return.
            epicurve = Optional.of(countyToCurveMap);

        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern " + getPattern(), e);
        }

        return epicurve;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

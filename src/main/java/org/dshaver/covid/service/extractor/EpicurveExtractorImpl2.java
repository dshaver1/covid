package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.find;

@Component
public class EpicurveExtractorImpl2 extends AbstractExtractor implements Extractor<String, Map<String, Epicurve>> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurveExtractorImpl2.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\[\\{\"measure\".+?}])");
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);
    public static final List<String> COUNTY_FILTER = Arrays.asList("georgia", "cobb", "fulton", "gwinnett", "dekalb", "carroll", "unknown");

    @Inject
    protected EpicurveExtractorImpl2(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<Map<String, Epicurve>> extract(List<String> raw, String id) {
        logger.info("Extracting data from " + id);
        Optional<String> epicurveString = find(raw, getPattern());
        List<EpicurvePointImpl2> epicurvePoints = null;
        Optional<Map<String, Epicurve>> epicurve = Optional.empty();
        Map<LocalDate, Integer> caseTotals = new HashMap<>();
        Map<LocalDate, Integer> deathTotals = new HashMap<>();

        try {
            if (epicurveString.isPresent()) {
                Map<String, Collection<EpicurvePoint>> filteredDataPoints = new HashMap<>();
                // First we need to parse the json into my DTO object
                epicurvePoints = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<EpicurvePointImpl2>>() {
                });
                for (EpicurvePointImpl2 current : epicurvePoints) {
                    LocalDate labelDate = LocalDate.parse(current.getTestDate(), DateTimeFormatter.ISO_DATE);
                    // Next iterate over the points and filter/decorate as needed.
                    if (labelDate.isAfter(EARLIEST_DATE) && COUNTY_FILTER.contains(current.getCounty().toLowerCase())) {
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

            }
        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern {}", getPattern());
        }

        return epicurve;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

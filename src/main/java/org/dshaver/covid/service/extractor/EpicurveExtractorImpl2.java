package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.dshaver.covid.dao.TestingStatsRepository;
import org.dshaver.covid.domain.epicurve.*;
import org.dshaver.covid.service.CountyService;
import org.dshaver.covid.service.CsvService;
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
public class EpicurveExtractorImpl2 extends AbstractExtractor implements Extractor<EpicurvePointImpl2, Map<String, Epicurve>> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurveExtractorImpl2.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\[\\{\"measure\".+?}])");
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);

    private final TestingStatsRepository testingStatsRepository;
    private final CountyService countyService;

    @Inject
    protected EpicurveExtractorImpl2(ObjectMapper objectMapper,
                                     TestingStatsRepository testingStatsRepository,
                                     CountyService countyService) {
        super(objectMapper);
        this.testingStatsRepository = testingStatsRepository;
        this.countyService = countyService;
    }

    @Override
    public Optional<Map<String, Epicurve>> extract(List<EpicurvePointImpl2> raw, String id) {
        logger.info("Extracting data from " + id);
        // Add testing stats if available.
        Optional<Table<String, LocalDate, TestingStatsDto>> maybeTestingStatsMap = getTestingStats(id);
        Optional<Map<String, Epicurve>> epicurve = Optional.empty();
        Map<LocalDate, Integer> caseTotals = new HashMap<>();
        Map<LocalDate, Integer> deathTotals = new HashMap<>();

        try {
            Map<String, Collection<EpicurvePoint>> filteredDataPoints = new HashMap<>();

            for (EpicurvePointImpl2 current : raw) {
                LocalDate labelDate = LocalDate.parse(current.getTestDate(), DateTimeFormatter.ISO_DATE);
                // Next iterate over the points and filter/decorate as needed.
                if (labelDate.isAfter(EARLIEST_DATE) && countyService.isCountyEnabled(current.getCounty())) {
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

                    maybeTestingStatsMap.ifPresent(map -> {
                        TestingStatsDto dto = map.get(current.getCounty(), current.getLabelDate());
                        if (dto != null) {
                            current.setPcrPos(dto.getPcrPos());
                            current.setPcrTest(dto.getPcrTest());
                            current.setDay7PerPcrPos(dto.getDay7PerPcrPos());
                            current.setDay14PerPcrPos(dto.getDay14PerPcrPos());
                        }
                    });
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
            logger.info("Could not find epicurve with pattern {}", getPattern());
        }

        return epicurve;
    }

    private Optional<Table<String, LocalDate, TestingStatsDto>> getTestingStats(String id) {
        Optional<Table<String, LocalDate, TestingStatsDto>> optionalMap = Optional.empty();
        Optional<TestingStatsContainer> testingStatsContainer = testingStatsRepository.findById(id);

        if (testingStatsContainer.isPresent()) {
            Table<String, LocalDate, TestingStatsDto> table = testingStatsContainer.get()
                    .getPayload()
                    .stream()
                    .filter(dto -> countyService.isCountyEnabled(dto.getCounty().toLowerCase()))
                    .collect(Tables.toTable(TestingStatsDto::getCounty,
                            TestingStatsDto::getReportDate,
                            dto -> dto,
                            (dto1, dto2) -> new TestingStatsDto(),
                            HashBasedTable::create));

            optionalMap = Optional.of(table);
        }

        return optionalMap;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurveDtoImpl1;
import org.dshaver.covid.domain.epicurve.EpicurvePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.find;

@Component
public class EpicurveExtractorImpl1 extends AbstractExtractor implements Extractor<String, Map<String, Epicurve>> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurveExtractorImpl1.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+EPICURVE\".+?}]})");
    private static final DateTimeFormatter SOURCE_LABEL_FORMAT_V2 = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ddMMMyyyy").toFormatter();
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);

    @Inject
    protected EpicurveExtractorImpl1(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<Map<String, Epicurve>> extract(List<String> raw, String id) {
        Optional<String> epicurveString = find(raw, getPattern());
        Optional<Map<String, Epicurve>> maybeEpicurve = Optional.empty();
        EpicurveDtoImpl1 epicurve = null;
        try {
            if (epicurveString.isPresent()) {
                List<EpicurvePoint> filteredDataPoints = new ArrayList<>();
                epicurve = getObjectMapper().readValue(epicurveString.get(), EpicurveDtoImpl1.class);
                for (EpicurvePoint current : epicurve.getEpicurvePoints()) {
                    LocalDate labelDate = LocalDate.parse(current.getTestDate(), SOURCE_LABEL_FORMAT_V2);
                    if (labelDate.isAfter(EARLIEST_DATE)) {
                        current.setSource(id);
                        current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                        current.setLabelDate(labelDate);
                        filteredDataPoints.add(current);
                    }
                }
                epicurve.setEpicurvePoints(filteredDataPoints);
                Map<String, Epicurve> tempMap = new HashMap<>();
                tempMap.put("georgia", new Epicurve("Georgia", epicurve.getEpicurvePoints()));
                maybeEpicurve = Optional.of(tempMap);
            }
        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern {}", getPattern());
        }

        return maybeEpicurve;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

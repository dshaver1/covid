package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.BaseFileRepository;
import org.dshaver.covid.domain.epicurve.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.find;

@Component
public class EpicurvePointImpl1Extractor extends AbstractExtractor implements Extractor<String, EpicurveDtoImpl1> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurvePointImpl1Extractor.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+EPICURVE\".+?}]})");
    private static final DateTimeFormatter SOURCE_LABEL_FORMAT_V2 = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("ddMMMyyyy").toFormatter();
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);

    @Inject
    protected EpicurvePointImpl1Extractor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<EpicurveDtoImpl1> extract(List<String> raw, String id) {
        Optional<String> epicurveString = find(raw, getPattern());
        EpicurveDtoImpl1 container;
        Optional<EpicurveDtoImpl1> optionalContainer = Optional.empty();
        try {
            if (epicurveString.isPresent()) {
                container = getObjectMapper().readValue(epicurveString.get(), EpicurveDtoImpl1.class);
                container.setId(id);
                container.setReportDate(LocalDateTime.parse(id, BaseFileRepository.idFormatter).toLocalDate());
                for (EpicurvePoint current : container.getEpicurvePoints()) {
                    LocalDate labelDate = LocalDate.parse(current.getTestDate(), SOURCE_LABEL_FORMAT_V2);
                    if (labelDate.isAfter(EARLIEST_DATE)) {
                        current.setSource(id);
                        current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                        current.setLabelDate(labelDate);
                    }
                }
                optionalContainer = Optional.of(container);
            }
        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern {}", getPattern());
        }

        return optionalContainer;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

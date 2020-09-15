package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.BaseFileRepository;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPoint;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.find;

@Component
public class HealthcareWorkerDtoExtractor extends AbstractExtractor implements Extractor<String, HealthcareWorkerEpiPointContainer> {
    private static final Logger logger = LoggerFactory.getLogger(HealthcareWorkerDtoExtractor.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\[\\{\"test_date\":\"\\d{4}-\\d{2}-\\d{2}\",\"ethnicity\".+?}])");

    @Inject
    protected HealthcareWorkerDtoExtractor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<HealthcareWorkerEpiPointContainer> extract(List<String> raw, String id) {
        Optional<String> epicurveString = find(raw, getPattern());
        List<HealthcareWorkerEpiPoint> epicurvePoints = null;
        Optional<HealthcareWorkerEpiPointContainer> optionalContainer = Optional.empty();

        try {
            if (epicurveString.isPresent()) {
                epicurvePoints = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<HealthcareWorkerEpiPoint>>() {
                });

                HealthcareWorkerEpiPointContainer container = new HealthcareWorkerEpiPointContainer();
                container.setPayload(epicurvePoints);
                container.setId(id);
                container.setReportDate(LocalDateTime.parse(id.replace(":", ""), BaseFileRepository.idFormatter).toLocalDate());

                optionalContainer = Optional.of(container);
            }
        } catch (Exception e) {
            logger.info("Could not find healthcare epicurve with pattern {}", getPattern());
        }

        return optionalContainer;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

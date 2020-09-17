package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.BaseFileRepository;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
import org.dshaver.covid.domain.epicurve.TestingStatsContainer;
import org.dshaver.covid.domain.epicurve.TestingStatsDto;
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
public class TestingStatsExtractor extends AbstractExtractor implements Extractor<String, TestingStatsContainer>{
    private static final Logger logger = LoggerFactory.getLogger(TestingStatsExtractor.class);
    private static final Pattern testStatsPattern = Pattern.compile("(\\[\\{[^\\[\\]]+?day7_per_pcrpos.+?}])");

    @Inject
    protected TestingStatsExtractor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<TestingStatsContainer> extract(List<String> raw, String id) {
        logger.info("Extracting TestingStatsContainer for {}...", id);

        Optional<String> epicurveString = find(raw, getPattern());
        List<TestingStatsDto> dtos = null;
        Optional<TestingStatsContainer> optionalContainer = Optional.empty();
        try {
            if (epicurveString.isPresent()) {
                dtos = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<TestingStatsDto>>() {
                });

                TestingStatsContainer container = new TestingStatsContainer();
                container.setPayload(dtos);
                container.setId(id);
                container.setReportDate(LocalDateTime.parse(id.replace(":", ""), BaseFileRepository.idFormatter).toLocalDate());

                optionalContainer = Optional.of(container);
            } else {
                logger.info("No testing stats available for {}!", id);
            }
        } catch (Exception e) {
            logger.info("Could not find testing stats dto with pattern " + getPattern(), e);
        }

        return optionalContainer;
    }

    @Override
    public Pattern getPattern() {
        return testStatsPattern;
    }
}

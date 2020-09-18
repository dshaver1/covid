package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.dao.BaseFileRepository;
import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
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
public class EpicurvePointImpl2Extractor extends AbstractExtractor implements Extractor<String, EpicurvePointImpl2Container> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurvePointImpl2Extractor.class);
    private static final Pattern epicurvePattern = Pattern.compile("(\\[\\{\"measure\":\"state_total\",\"county\":\"\\D+\",\"test_date\".+?}])");

    @Inject
    protected EpicurvePointImpl2Extractor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<EpicurvePointImpl2Container> extract(List<String> raw, String id) {
        logger.info("Extracting EpicurvePointImpl2Container for id {}", id);
        Optional<String> epicurveString = find(raw, getPattern());
        List<EpicurvePointImpl2> epicurvePoints = null;
        Optional<EpicurvePointImpl2Container> optionalContainer = Optional.empty();

        try {
            if (epicurveString.isPresent()) {
                epicurvePoints = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<EpicurvePointImpl2>>() {
                });

                EpicurvePointImpl2Container container = new EpicurvePointImpl2Container();
                container.setPayload(epicurvePoints);
                container.setId(id);
                container.setReportDate(LocalDateTime.parse(id.replace(":", ""), BaseFileRepository.idFormatter).toLocalDate());

                optionalContainer = Optional.of(container);
            }
        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern " + getPattern(), e);
        }

        return optionalContainer;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

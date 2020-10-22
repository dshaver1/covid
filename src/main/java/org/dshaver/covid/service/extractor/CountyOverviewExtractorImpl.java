package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.BaseFileRepository;
import org.dshaver.covid.domain.CountyOverview;
import org.dshaver.covid.domain.CountyOverviewContainer;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPoint;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
import org.dshaver.covid.domain.overview.ReportOverview;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
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
public class CountyOverviewExtractorImpl extends AbstractExtractor implements Extractor<String, CountyOverviewContainer> {
    private static final Logger logger = LoggerFactory.getLogger(CountyOverviewExtractorImpl.class);
    private static final Pattern overviewPattern = Pattern.compile("(\\[\\{\"county_name\".+?}]|\\[\\{\"county_resident\".+?}])");

    @Inject
    protected CountyOverviewExtractorImpl(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<CountyOverviewContainer> extract(List<String> raw, String id) {
        logger.info("Extracting CountyOverview for id {}", id);
        Optional<String> epicurveString = find(raw, getPattern());
        List<CountyOverview> epicurvePoints = null;
        Optional<CountyOverviewContainer> optionalContainer = Optional.empty();

        try {
            if (epicurveString.isPresent()) {
                epicurvePoints = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<CountyOverview>>() {
                });

                CountyOverviewContainer container = new CountyOverviewContainer();
                container.setPayload(epicurvePoints);
                container.setId(id);
                container.setReportDate(LocalDateTime.parse(id.replace(":", ""), BaseFileRepository.idFormatter).toLocalDate());

                optionalContainer = Optional.of(container);
            }
        } catch (Exception e) {
            logger.info("Could not find CountyOverview data with pattern {}", getPattern());
        }

        return optionalContainer;
    }

    @Override
    public Pattern getPattern() {
        return overviewPattern;
    }
}

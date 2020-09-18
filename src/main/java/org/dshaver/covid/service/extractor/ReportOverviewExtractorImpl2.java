package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.dao.BaseFileRepository;
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
public class ReportOverviewExtractorImpl2 extends AbstractExtractor implements Extractor<String, ReportOverview> {
    private static final Logger logger = LoggerFactory.getLogger(ReportOverviewExtractorImpl2.class);
    private static final Pattern overviewPattern = Pattern.compile("(\\[\\{\"total_tests\".+?}])");

    @Inject
    protected ReportOverviewExtractorImpl2(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<ReportOverview> extract(List<String> raw, String id) {
        logger.info("Extracting ReportOverview for id {}", id);
        Optional<ReportOverview> reportOverview = Optional.empty();
        Optional<String> overviewString = find(raw, getPattern());
        List<ReportOverviewImpl2> reportOverviewContainer = null;

        try {
            if (overviewString.isPresent()) {
                reportOverviewContainer = getObjectMapper().readValue(overviewString.get(), new TypeReference<List<ReportOverviewImpl2>>(){});
                if (reportOverviewContainer != null && !reportOverviewContainer.isEmpty()) {
                    ReportOverviewImpl2 overview = reportOverviewContainer.get(0);
                    overview.setId(id);
                    overview.setReportDate(LocalDateTime.parse(id, BaseFileRepository.idFormatter).toLocalDate());
                    reportOverview = Optional.of(overview);
                }
            }
        } catch (Exception e) {
            logger.info("Could not find report overview with pattern {}", getPattern());
        }

        return reportOverview;
    }

    @Override
    public Pattern getPattern() {
        return overviewPattern;
    }
}

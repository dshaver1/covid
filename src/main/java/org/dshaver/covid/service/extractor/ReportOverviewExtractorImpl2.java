package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.overview.ReportOverview;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.getVarFromRegex;

@Component
public class ReportOverviewExtractorImpl2 extends AbstractExtractor implements Extractor<String, ReportOverview> {
    private static final Logger logger = LoggerFactory.getLogger(ReportOverviewExtractorImpl2.class);
    private static final Pattern overviewPattern = Pattern.compile(".*JSON.parse\\('(\\[\\{\"total_tests\".+?]'\\)}).*");

    @Inject
    protected ReportOverviewExtractorImpl2(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<ReportOverview> extract(List<String> raw, String id) {
        Optional<ReportOverview> reportOverview = Optional.empty();
        Optional<String> overviewString = getVarFromRegex(raw, getPattern());
        List<ReportOverviewImpl2> reportOverviewContainer = null;

        try {
            if (overviewString.isPresent()) {
                reportOverviewContainer = getObjectMapper().readValue(overviewString.get(), new TypeReference<List<ReportOverviewImpl2>>(){});
                if (reportOverviewContainer != null && !reportOverviewContainer.isEmpty()) {
                    reportOverview = Optional.ofNullable(reportOverviewContainer.get(0));
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

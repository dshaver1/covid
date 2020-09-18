package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.overview.ReportOverview;
import org.dshaver.covid.domain.overview.ReportOverviewContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.find;

@Component
public class ReportOverviewExtractorImpl1 extends AbstractExtractor implements Extractor<String, ReportOverview> {
    private static final Logger logger = LoggerFactory.getLogger(ReportOverviewExtractorImpl1.class);
    private static final Pattern overviewPattern = Pattern.compile("(\\{\"SASJSONExport\":\"\\d\\.\\d.+?\",\"SASTableData\\+GA_COVID19_OVERALL\".+?}]})");

    @Inject
    protected ReportOverviewExtractorImpl1(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<ReportOverview> extract(List<String> raw, String id) {
        logger.info("Extracting ReportOverview for id {}", id);
        Optional<ReportOverview> reportOverview = Optional.empty();
        Optional<String> overviewString = find(raw, getPattern());
        ReportOverviewContainer reportOverviewContainer = null;

        try {
            if (overviewString.isPresent()) {
                reportOverviewContainer = getObjectMapper().readValue(overviewString.get(), ReportOverviewContainer.class);
                if (reportOverviewContainer != null) {
                    reportOverview = Optional.ofNullable(reportOverviewContainer.getReportOverviewList().get(0));
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

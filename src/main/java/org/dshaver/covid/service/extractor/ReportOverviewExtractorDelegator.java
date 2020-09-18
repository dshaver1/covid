package org.dshaver.covid.service.extractor;

import org.dshaver.covid.domain.overview.ReportOverview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReportOverviewExtractorDelegator implements Extractor<String, ReportOverview> {
    private static final Logger logger = LoggerFactory.getLogger(ReportOverviewExtractorDelegator.class);
    private final ReportOverviewExtractorImpl1 impl1;
    private final ReportOverviewExtractorImpl2 impl2;

    public ReportOverviewExtractorDelegator(ReportOverviewExtractorImpl1 impl1,
                                            ReportOverviewExtractorImpl2 impl2) {
        this.impl1 = impl1;
        this.impl2 = impl2;
    }

    @Override
    public Optional<ReportOverview> extract(List<String> raw, String id) {
        logger.info("Extracting ReportOverview for id {}", id);
        Optional<ReportOverview> overview = impl2.extract(raw, id);
        if (overview.isPresent()) {
            return overview;
        }

        overview = impl1.extract(raw, id);
        if (overview.isPresent()) {
            return overview;
        }

        return Optional.empty();
    }
}

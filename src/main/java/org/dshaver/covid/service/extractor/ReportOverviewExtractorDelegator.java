package org.dshaver.covid.service.extractor;

import org.dshaver.covid.domain.overview.ReportOverview;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ReportOverviewExtractorDelegator implements Extractor<String, ReportOverview> {
    private final ReportOverviewExtractorImpl1 impl1;
    private final ReportOverviewExtractorImpl2 impl2;

    public ReportOverviewExtractorDelegator(ReportOverviewExtractorImpl1 impl1,
                                            ReportOverviewExtractorImpl2 impl2) {
        this.impl1 = impl1;
        this.impl2 = impl2;
    }

    @Override
    public Optional<ReportOverview> extract(List<String> raw, String id) {
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

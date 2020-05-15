package org.dshaver.covid.service.extractor;

import org.dshaver.covid.domain.epicurve.Epicurve;
import org.dshaver.covid.domain.epicurve.EpicurveDto;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class EpicurveExtractorDelegator implements Extractor<String, Map<String, Epicurve>> {
    private final EpicurveExtractorImpl1 impl1;
    private final EpicurveExtractorImpl2 impl2;

    @Inject
    public EpicurveExtractorDelegator(EpicurveExtractorImpl1 impl1, EpicurveExtractorImpl2 impl2) {
        this.impl1 = impl1;
        this.impl2 = impl2;
    }

    @Override
    public Optional<Map<String, Epicurve>> extract(List<String> raw, String id) {
        Optional<Map<String, Epicurve>> epicurve = impl2.extract(raw, id);
        if (epicurve.isPresent()) {
            return epicurve;
        }

        epicurve = impl1.extract(raw, id);
        if (epicurve.isPresent()) {
            return epicurve;
        }

        return Optional.empty();
    }
}

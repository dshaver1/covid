package org.dshaver.covid.service;

import org.dshaver.covid.dao.EpicurveDtoRepository;
import org.dshaver.covid.dao.HealthcareDtoRepository;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
import org.dshaver.covid.service.extractor.EpicurvePointImpl2Extractor;
import org.dshaver.covid.service.extractor.HealthcareWorkerDtoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

@Service
public class IntermediaryDataService {
    private static final Logger logger = LoggerFactory.getLogger(IntermediaryDataService.class);

    private final EpicurvePointImpl2Extractor epicurvePointImpl2Extractor;
    private final EpicurveDtoRepository epicurveDtoRepository;
    private final HealthcareWorkerDtoExtractor healthcareWorkerDtoExtractor;
    private final HealthcareDtoRepository healthcareDtoRepository;

    @Inject
    public IntermediaryDataService(EpicurvePointImpl2Extractor epicurvePointImpl2Extractor,
                                   EpicurveDtoRepository epicurveDtoRepository,
                                   HealthcareWorkerDtoExtractor healthcareWorkerDtoExtractor,
                                   HealthcareDtoRepository healthcareDtoRepository) {
        this.epicurvePointImpl2Extractor = epicurvePointImpl2Extractor;
        this.epicurveDtoRepository = epicurveDtoRepository;
        this.healthcareWorkerDtoExtractor = healthcareWorkerDtoExtractor;
        this.healthcareDtoRepository = healthcareDtoRepository;
    }

    public void saveAll(RawData rawData) {
        Optional<EpicurvePointImpl2Container> epicurveContainer = epicurvePointImpl2Extractor.extract(rawData.getPayload(), rawData.getId());
        epicurveContainer.ifPresent(container -> {
            try {
                epicurveDtoRepository.save(container);
            } catch (IOException e) {
                logger.error("Error saving intermediary epicurve data with id " + rawData.getId(), e);
            }
        });

        Optional<HealthcareWorkerEpiPointContainer> healthcareContainer = healthcareWorkerDtoExtractor.extract(rawData.getPayload(), rawData.getId());
        healthcareContainer.ifPresent(container -> {
            try {
                healthcareDtoRepository.save(container);
            } catch (IOException e) {
                logger.error("Error saving intermediary healthcare data with id " + rawData.getId(), e);
            }
        });
    }
}

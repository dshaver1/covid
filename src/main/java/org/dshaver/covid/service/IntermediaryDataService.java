package org.dshaver.covid.service;

import org.dshaver.covid.dao.EpicurveDtoV1Repository;
import org.dshaver.covid.dao.EpicurveDtoV2Repository;
import org.dshaver.covid.dao.HealthcareDtoRepository;
import org.dshaver.covid.dao.ReportOverviewRepositoryV1;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.RawDataV2;
import org.dshaver.covid.domain.epicurve.EpicurveDtoImpl1;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
import org.dshaver.covid.domain.overview.ReportOverview;
import org.dshaver.covid.domain.overview.ReportOverviewContainer;
import org.dshaver.covid.domain.overview.ReportOverviewImpl1;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
import org.dshaver.covid.service.extractor.EpicurvePointImpl1Extractor;
import org.dshaver.covid.service.extractor.EpicurvePointImpl2Extractor;
import org.dshaver.covid.service.extractor.HealthcareWorkerDtoExtractor;
import org.dshaver.covid.service.extractor.ReportOverviewExtractorDelegator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

@Service
public class IntermediaryDataService {
    private static final Logger logger = LoggerFactory.getLogger(IntermediaryDataService.class);

    private final EpicurvePointImpl1Extractor epicurvePointImpl1Extractor;
    private final EpicurvePointImpl2Extractor epicurvePointImpl2Extractor;
    private final EpicurveDtoV1Repository epicurveDtoV1Repository;
    private final EpicurveDtoV2Repository epicurveDtoV2Repository;
    private final HealthcareWorkerDtoExtractor healthcareWorkerDtoExtractor;
    private final HealthcareDtoRepository healthcareDtoRepository;
    private final ReportOverviewExtractorDelegator overviewExtractorDelegator;
    private final ReportOverviewRepositoryV1 reportOverviewRepositoryV1;

    @Inject
    public IntermediaryDataService(EpicurvePointImpl1Extractor epicurvePointImpl1Extractor,
                                   EpicurvePointImpl2Extractor epicurvePointImpl2Extractor,
                                   EpicurveDtoV1Repository epicurveDtoV1Repository,
                                   EpicurveDtoV2Repository epicurveDtoV2Repository,
                                   HealthcareWorkerDtoExtractor healthcareWorkerDtoExtractor,
                                   HealthcareDtoRepository healthcareDtoRepository,
                                   ReportOverviewExtractorDelegator overviewExtractorDelegator,
                                   ReportOverviewRepositoryV1 reportOverviewRepositoryV1) {
        this.epicurvePointImpl1Extractor = epicurvePointImpl1Extractor;
        this.epicurvePointImpl2Extractor = epicurvePointImpl2Extractor;
        this.epicurveDtoV1Repository = epicurveDtoV1Repository;
        this.epicurveDtoV2Repository = epicurveDtoV2Repository;
        this.healthcareWorkerDtoExtractor = healthcareWorkerDtoExtractor;
        this.healthcareDtoRepository = healthcareDtoRepository;
        this.overviewExtractorDelegator = overviewExtractorDelegator;
        this.reportOverviewRepositoryV1 = reportOverviewRepositoryV1;
    }

    public void saveAll(RawData rawData) {
        if (rawData instanceof RawDataV1) {
            Optional<EpicurveDtoImpl1> epicurveContainer = epicurvePointImpl1Extractor.extract(rawData.getPayload(), rawData.getId());
            epicurveContainer.ifPresent(container -> {
                try {
                    epicurveDtoV1Repository.save(container);
                } catch (IOException e) {
                    logger.error("Error saving intermediary epicurve data with id " + rawData.getId(), e);
                }
            });
        }

        if (rawData instanceof RawDataV2) {
            Optional<EpicurvePointImpl2Container> epicurveContainer = epicurvePointImpl2Extractor.extract(rawData.getPayload(), rawData.getId());
            epicurveContainer.ifPresent(container -> {
                try {
                    epicurveDtoV2Repository.save(container);
                } catch (IOException e) {
                    logger.error("Error saving intermediary epicurve data with id " + rawData.getId(), e);
                }
            });
        }

        Optional<HealthcareWorkerEpiPointContainer> healthcareContainer = healthcareWorkerDtoExtractor.extract(rawData.getPayload(), rawData.getId());
        healthcareContainer.ifPresent(container -> {
            try {
                healthcareDtoRepository.save(container);
            } catch (IOException e) {
                logger.error("Error saving intermediary healthcare data with id " + rawData.getId(), e);
            }
        });

        Optional<ReportOverview> overviewContainer = overviewExtractorDelegator.extract(rawData.getPayload(), rawData.getId());
        overviewContainer.ifPresent(container -> {
            try {
                reportOverviewRepositoryV1.save((ReportOverviewImpl2)container);
            } catch (IOException e) {
                logger.error("Error saving intermediary healthcare data with id " + rawData.getId(), e);
            }
        });
    }
}

package org.dshaver.covid.dao;

import org.dshaver.covid.domain.ManualRawData;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.RawDataV2;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RawDataRepositoryDelegator {
    private final RawDataRepositoryV1 rawDataRepositoryV1;
    private final RawDataRepositoryV2 rawDataRepositoryV2;
    private final ManualRawDataRepository manualRawDataRepository;

    @Inject
    public RawDataRepositoryDelegator(RawDataRepositoryV1 rawDataRepositoryV1,
                                      RawDataRepositoryV2 rawDataRepositoryV2,
                                      ManualRawDataRepository manualRawDataRepository) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
        this.manualRawDataRepository = manualRawDataRepository;
    }

    public RawData save(RawData entity) {
        try {
            if (entity instanceof RawDataV1) {
                return rawDataRepositoryV1.save((RawDataV1) entity);
            } else if (entity instanceof RawDataV2) {
                return rawDataRepositoryV2.save((RawDataV2) entity);
            } else if (entity instanceof ManualRawData) {
                return manualRawDataRepository.save((ManualRawData) entity);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return entity;
    }

    public void deleteAll(Class<? extends RawData> rawDataClass) {
        if (rawDataClass.equals(RawDataV1.class)) {
            rawDataRepositoryV1.deleteAll();
        } else if (rawDataClass.equals(RawDataV2.class)) {
            rawDataRepositoryV2.deleteAll();
        } else if (rawDataClass.equals(ManualRawData.class)) {
            manualRawDataRepository.deleteAll();
        }
    }

    public List<RawData> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate, Class<? extends RawData> rawDataClass) {
        if (rawDataClass.equals(RawDataV1.class)) {
            return rawDataRepositoryV1.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList());
        } else if (rawDataClass.equals(RawDataV2.class)) {
            return rawDataRepositoryV2.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList());
        } else if (rawDataClass.equals(ManualRawData.class)) {
            return manualRawDataRepository.findByReportDateBetweenOrderByIdAsc(startDate, endDate).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}

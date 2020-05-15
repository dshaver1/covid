package org.dshaver.covid.dao;

import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.RawDataV2;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class RawDataRepositoryDelegator {
    private final RawDataRepositoryV1 rawDataRepositoryV1;
    private final RawDataRepositoryV2 rawDataRepositoryV2;

    @Inject
    public RawDataRepositoryDelegator(RawDataRepositoryV1 rawDataRepositoryV1,
                                      RawDataRepositoryV2 rawDataRepositoryV2) {
        this.rawDataRepositoryV1 = rawDataRepositoryV1;
        this.rawDataRepositoryV2 = rawDataRepositoryV2;
    }

    public RawData save(RawData entity) {
        if (entity instanceof RawDataV1) {
            return rawDataRepositoryV1.save((RawDataV1)entity);
        }
        else if (entity instanceof RawDataV2) {
            return rawDataRepositoryV2.save((RawDataV2)entity);
        }

        return entity;
    }

    public List<RawData> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate, Class<? extends RawData> rawDataClass) {
        if (rawDataClass.equals(RawDataV1.class)) {
            return rawDataRepositoryV1.findByReportDateBetweenOrderByIdAsc(startDate, endDate);
        }
        else if (rawDataClass.equals(RawDataV2.class)) {
            return rawDataRepositoryV2.findByReportDateBetweenOrderByIdAsc(startDate, endDate);
        }

        return new ArrayList<>();
    }
}

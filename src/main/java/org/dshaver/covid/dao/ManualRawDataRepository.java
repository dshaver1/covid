package org.dshaver.covid.dao;

import org.dshaver.covid.domain.ManualRawData;
import org.dshaver.covid.domain.RawData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
public interface ManualRawDataRepository extends MongoRepository<ManualRawData, String> {

    List<RawData> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate);
}

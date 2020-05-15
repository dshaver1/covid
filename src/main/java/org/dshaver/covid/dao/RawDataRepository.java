package org.dshaver.covid.dao;

import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
public interface RawDataRepository extends MongoRepository<RawDataV1, String> {

    List<RawDataV1> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate);

}

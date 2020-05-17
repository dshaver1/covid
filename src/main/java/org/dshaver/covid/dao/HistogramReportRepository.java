package org.dshaver.covid.dao;

import org.dshaver.covid.domain.HistogramReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HistogramReportRepository extends MongoRepository<HistogramReport, String> {
    List<HistogramReport> findAllByOrderByIdDesc();

    List<HistogramReport> findAllByOrderByIdAsc();
}

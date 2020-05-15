package org.dshaver.covid.dao;

import org.dshaver.covid.domain.HistogramReport;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistogramReportRepository extends MongoRepository<HistogramReport, String> {
}

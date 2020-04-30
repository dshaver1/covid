package org.dshaver.covid.dao;

import org.dshaver.covid.domain.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
public interface ReportRepository extends MongoRepository<Report, String> {
    List<Report> findAllByOrderByIdAsc();

    List<Report> findByReportDateOrderByIdAsc(LocalDate reportDate);

    List<Report> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate);
}

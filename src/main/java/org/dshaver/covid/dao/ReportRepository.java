package org.dshaver.covid.dao;

import org.dshaver.covid.domain.Report;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class ReportRepository extends BaseFileDao<Report> {
    public List<Report> findAllByOrderByIdAsc() {
        return null;
    }

    public List<Report> findByReportDateOrderByIdAsc(LocalDate reportDate) {
        return null;
    }

    public List<Report> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Override
    public Path getPath() {
        return null;
    }
}

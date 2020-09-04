package org.dshaver.covid.dao;

import org.dshaver.covid.domain.HistogramReport;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class HistogramReportRepository extends BaseFileDao<HistogramReport> {
    public List<HistogramReport> findAllByOrderByIdDesc() {
        return null;
    }

    public List<HistogramReport> findAllByOrderByIdAsc() {
        return null;
    }

    @Override
    public Path getPath() {
        return null;
    }
}

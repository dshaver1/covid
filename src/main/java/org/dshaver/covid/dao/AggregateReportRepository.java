package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.AggregateReport;
import org.dshaver.covid.domain.HistogramReportContainer;
import org.dshaver.covid.service.FileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AggregateReportRepository extends BaseFileRepository<AggregateReport> {
    private static final Logger logger = LoggerFactory.getLogger(AggregateReportRepository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public AggregateReportRepository(@Value("${covid.dirs.reports.aggregate}") String histogramPath,
                                     FileRegistry fileRegistry,
                                     ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(histogramPath));
        this.fileRegistry = fileRegistry;
    }

    public List<AggregateReport> findAllByOrderByIdDesc() {
        return new ArrayList<>();
    }

    @Override
    public String createFilename(AggregateReport report) {
        return String.format("AGGREGATE_%s.json", reportDateFormatter.format(report.getReportDate()));
    }

    @Override
    public Class<AggregateReport> getClazz() {
        return AggregateReport.class;
    }
}

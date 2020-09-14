package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.HistogramReport;
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
public class HistogramReportRepository extends BaseFileRepository<HistogramReport> {
    private static final Logger logger = LoggerFactory.getLogger(HistogramReport.class);
    public final FileRegistry fileRegistry;

    @Inject
    public HistogramReportRepository(@Value("${covid.dirs.histogram}") String histogramPath,
                                     FileRegistry fileRegistry,
                                     ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(histogramPath));
        this.fileRegistry = fileRegistry;
    }

    public List<HistogramReport> findAllByOrderByIdDesc() {
        return new ArrayList<>();
    }

    @Override
    public String createFilename(HistogramReport report) {
        return String.format("histogram_%s.json", timeFormatter.format(report.getReportDate()));
    }

    @Override
    public Class<HistogramReport> getClazz() {
        return HistogramReport.class;
    }
}

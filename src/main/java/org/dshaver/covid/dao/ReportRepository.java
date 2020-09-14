package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.service.FileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.nio.file.Paths;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class ReportRepository extends BaseFileRepository<Report> {
    private static final Logger logger = LoggerFactory.getLogger(ReportRepository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public ReportRepository(@Value("${covid.dirs.report.target.json}") String path,
                               FileRegistry fileRegistry,
                               ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(Report entity) {
        return String.format("REPORT_V2_%s.json", timeFormatter.format(entity.getReportDate()));
    }

    @Override
    public Class<Report> getClazz() {
        return Report.class;
    }
}

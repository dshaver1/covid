package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.RawDataV0;
import org.dshaver.covid.domain.overview.ReportOverviewContainer;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
import org.dshaver.covid.service.FileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class ReportOverviewRepositoryV1 extends BaseFileRepository<ReportOverviewImpl2> {
    private static final Logger logger = LoggerFactory.getLogger(ReportOverviewRepositoryV1.class);
    public final FileRegistry fileRegistry;

    @Inject
    public ReportOverviewRepositoryV1(@Value("${covid.dirs.filtered.overview.v1}") String path,
                                      FileRegistry fileRegistry,
                                      ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(ReportOverviewImpl2 entity) {
        return String.format("REPORT_OVERVIEW_V1_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<ReportOverviewImpl2> getClazz() {
        return ReportOverviewImpl2.class;
    }
}

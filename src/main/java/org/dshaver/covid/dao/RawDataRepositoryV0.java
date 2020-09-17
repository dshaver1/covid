package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.RawDataV0;
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
public class RawDataRepositoryV0 extends BaseFileRepository<RawDataV0> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataRepositoryV0.class);
    public final FileRegistry fileRegistry;

    @Inject
    public RawDataRepositoryV0(@Value("${covid.dirs.raw.v0}") String path,
                               FileRegistry fileRegistry,
                               ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(RawDataV0 entity) {
        return String.format("RAW_V0_%s.json", reportDateFormatter.format(entity.getReportDate()));
    }

    @Override
    public Class<RawDataV0> getClazz() {
        return RawDataV0.class;
    }
}

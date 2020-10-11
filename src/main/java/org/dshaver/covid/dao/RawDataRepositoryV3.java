package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.RawDataV3;
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
public class RawDataRepositoryV3 extends BaseFileRepository<RawDataV3> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataRepositoryV3.class);
    public final FileRegistry fileRegistry;

    @Inject
    public RawDataRepositoryV3(@Value("${covid.dirs.raw.v3}") String path,
                               FileRegistry fileRegistry,
                               ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(RawDataV3 entity) {
        return String.format("DPH_RAW_V3_%s.json", reportDateFormatter.format(entity.getReportDate()));
    }

    @Override
    public Class<RawDataV3> getClazz() {
        return RawDataV3.class;
    }
}

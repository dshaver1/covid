package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.RawDataV2;
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
public class RawDataRepositoryV2 extends BaseFileRepository<RawDataV2> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataRepositoryV2.class);
    public final FileRegistry fileRegistry;

    @Inject
    public RawDataRepositoryV2(@Value("covid.dirs.raw.v2") String path,
                               FileRegistry fileRegistry,
                               ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(RawDataV2 entity) {
        return String.format("RAW_V2_%s.json", timeFormatter.format(entity.getReportDate()));
    }

    @Override
    public Class<RawDataV2> getClazz() {
        return RawDataV2.class;
    }
}

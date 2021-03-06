package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.ManualRawData;
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
public class ManualRawDataRepository extends BaseFileRepository<ManualRawData> {
    private static final Logger logger = LoggerFactory.getLogger(ManualRawDataRepository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public ManualRawDataRepository(@Value("${covid.dirs.raw.manual}") String path,
                                     FileRegistry fileRegistry,
                                     ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(ManualRawData entity) {
        return String.format("MANUAL_RAW_%s.json", reportDateFormatter.format(entity.getReportDate()));
    }

    @Override
    public Class<ManualRawData> getClazz() {
        return ManualRawData.class;
    }
}

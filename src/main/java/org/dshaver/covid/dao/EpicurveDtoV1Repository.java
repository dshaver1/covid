package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.EpicurveDtoImpl1;
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
public class EpicurveDtoV1Repository extends BaseFileRepository<EpicurveDtoImpl1> {
    public final FileRegistry fileRegistry;

    @Inject
    public EpicurveDtoV1Repository(@Value("${covid.dirs.filtered.epicurve.v1}") String path,
                                   FileRegistry fileRegistry,
                                   ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(EpicurveDtoImpl1 entity) {
        return String.format("EPICURVE_DTO_V1_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<EpicurveDtoImpl1> getClazz() {
        return EpicurveDtoImpl1.class;
    }
}

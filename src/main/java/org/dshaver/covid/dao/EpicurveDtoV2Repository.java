package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2Container;
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
public class EpicurveDtoV2Repository extends BaseFileRepository<EpicurvePointImpl2Container> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurveDtoV2Repository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public EpicurveDtoV2Repository(@Value("${covid.dirs.filtered.epicurve.v2}") String path,
                                   FileRegistry fileRegistry,
                                   ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(EpicurvePointImpl2Container entity) {
        return String.format("EPICURVE_DTO_V2_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<EpicurvePointImpl2Container> getClazz() {
        return EpicurvePointImpl2Container.class;
    }
}

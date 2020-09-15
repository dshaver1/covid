package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.BasicFile;
import org.dshaver.covid.domain.Report;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPoint;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
import org.dshaver.covid.service.FileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class HealthcareDtoRepository extends BaseFileRepository<HealthcareWorkerEpiPointContainer> {
    private static final Logger logger = LoggerFactory.getLogger(HealthcareDtoRepository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public HealthcareDtoRepository(@Value("${covid.dirs.filtered.healthcare}") String path,
                                   FileRegistry fileRegistry,
                                   ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(HealthcareWorkerEpiPointContainer entity) {
        return String.format("HEALTHCARE_DTO_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<HealthcareWorkerEpiPointContainer> getClazz() {
        return HealthcareWorkerEpiPointContainer.class;
    }
}

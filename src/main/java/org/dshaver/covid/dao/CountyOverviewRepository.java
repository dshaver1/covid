package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.CountyOverviewContainer;
import org.dshaver.covid.domain.epicurve.HealthcareWorkerEpiPointContainer;
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
public class CountyOverviewRepository extends BaseFileRepository<CountyOverviewContainer> {
    private static final Logger logger = LoggerFactory.getLogger(CountyOverviewRepository.class);
    public final FileRegistry fileRegistry;

    @Inject
    public CountyOverviewRepository(@Value("${covid.dirs.filtered.countyOverview.v1}") String path,
                                    FileRegistry fileRegistry,
                                    ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(CountyOverviewContainer entity) {
        return String.format("COUNTY_OVERVIEW_DTO_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<CountyOverviewContainer> getClazz() {
        return CountyOverviewContainer.class;
    }
}

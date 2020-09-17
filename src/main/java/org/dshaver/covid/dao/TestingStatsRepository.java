package org.dshaver.covid.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.TestingStatsContainer;
import org.dshaver.covid.service.FileRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class TestingStatsRepository extends BaseFileRepository<TestingStatsContainer> {
    public final FileRegistry fileRegistry;

    @Inject
    public TestingStatsRepository(@Value("${covid.dirs.filtered.testingstats.v1}") String path,
                                  FileRegistry fileRegistry,
                                  ObjectMapper objectMapper) {
        super(objectMapper, fileRegistry, Paths.get(path));
        this.fileRegistry = fileRegistry;
    }

    @Override
    public String createFilename(TestingStatsContainer entity) {
        return String.format("TESTING_STATS_DTO_V1_%s.json", idFormatter.format(LocalDateTime.parse(entity.getId().replace(":", ""), idFormatter)));
    }

    @Override
    public Class<TestingStatsContainer> getClazz() {
        return TestingStatsContainer.class;
    }
}

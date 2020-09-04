package org.dshaver.covid.dao;

import org.dshaver.covid.domain.ManualRawData;
import org.dshaver.covid.domain.RawData;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Created by xpdf64 on 2020-04-27.
 */
@Repository
public class ManualRawDataRepository extends BaseFileDao<ManualRawData> {

    public List<RawData> findByReportDateBetweenOrderByIdAsc(LocalDate startDate, LocalDate endDate) {
        return null;
    }

    public List<RawData> findByReportDateOrderByIdAsc(LocalDate reportDate) {
        return null;
    }

    @Override
    public Path getPath() {
        return null;
    }
}

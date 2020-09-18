package org.dshaver.covid.domain;

import lombok.Data;
import org.dshaver.covid.domain.epicurve.EpicurvePointImpl2;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Data
public class CountyOverviewContainer implements Identifiable {
    private LocalDate reportDate;
    private String id;
    private Path filePath;
    private List<CountyOverview> payload;
}

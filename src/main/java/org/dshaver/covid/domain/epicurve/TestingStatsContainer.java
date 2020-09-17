package org.dshaver.covid.domain.epicurve;

import lombok.Data;
import org.dshaver.covid.domain.Identifiable;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Data
public class TestingStatsContainer implements Identifiable {
    private LocalDate reportDate;
    private String id;
    private Path filePath;
    private List<TestingStatsDto> payload;
}

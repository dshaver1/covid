package org.dshaver.covid.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class FileIndex {
    private LocalDateTime lastUpdated;
    private Class<? extends Identifiable> clazz;
    private final Map<String, Path> idToPath = new HashMap<>();
    private final Map<LocalDate,Path> reportDateToPath = new HashMap<>();

    public FileIndex() {
    }

    public void add(Identifiable entity) {
        idToPath.put(entity.getId(), entity.getFilePath());
        reportDateToPath.put(entity.getReportDate(), entity.getFilePath());
    }

    public void remove(Identifiable entity) {
        idToPath.remove(entity.getId());
        reportDateToPath.remove(entity.getReportDate());
    }
}

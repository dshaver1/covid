package org.dshaver.covid.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicFile implements Identifiable {
    private LocalDate reportDate;
    private String id;
    private Path filePath;
    private List<String> payload;
}

package org.dshaver.covid.domain;

import java.nio.file.Path;
import java.time.LocalDate;

public interface Identifiable {
    LocalDate getReportDate();

    String getId();

    Path getFilePath();

    void setFilePath(Path path);
}

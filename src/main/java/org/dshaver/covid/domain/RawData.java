package org.dshaver.covid.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface RawData extends Identifiable {
    String getId();

    LocalDateTime getCreateTime();

    LocalDate getReportDate();

    List<String> getPayload();
}

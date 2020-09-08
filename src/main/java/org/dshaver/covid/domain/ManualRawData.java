package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = {"payload"})
public class ManualRawData implements RawData {
    private String id;

    private LocalDateTime createTime;

    private LocalDate reportDate;

    private Path filePath;

    private int totalTests;

    private int confirmedCases;

    private int icu;

    private int hospitalizations;

    private int deaths;

    private List<String> payload;
}

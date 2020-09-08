package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a download of the source site (https://dph.georgia.gov/covid-19-daily-status-report) in its rawest form. Minimally parsed to get the
 * report date/time.
 */
@Data
@ToString(exclude = "lines")
public class RawDataV1 implements RawData {
    private String id;

    private LocalDateTime createTime;

    private LocalDate reportDate;

    private Path filePath;

    private List<String> lines;

    @JsonIgnore
    @Override
    public List<String> getPayload() {
        return lines;
    }
}

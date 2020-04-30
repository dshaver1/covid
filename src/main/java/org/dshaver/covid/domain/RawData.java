package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a download of the source site (https://dph.georgia.gov/covid-19-daily-status-report) in its rawest form. Minimally parsed to get the
 * report date/time.
 */
@Data
@ToString(exclude = "lines")
@Document("rawdata")
public class RawData {
    @Id
    private String id;

    private LocalDateTime createTime;

    private LocalDate reportDate;

    private List<String> lines;
}

package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Used to hold data from the new randomized URLs. These are populated in 3 steps:
 *
 * 1. Download HTML file from https://ga-covid19.ondemand.sas.com/
 * 2. Locate all linked URL's and filter down to the ones we care about such as https://ga-covid19.ondemand.sas.com/static/js/4.56e822ef.chunk.js and
 * https://ga-covid19.ondemand.sas.com/static/js/main.94c3fb67.chunk.js
 * 3. Download sources and save to payload field
 */
@Data
@ToString(exclude = {"payload"})
public class RawDataV3 implements RawData {
    private String id;

    private LocalDateTime createTime;

    private LocalDate reportDate;

    private Path filePath;

    private List<String> payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id.replace(":","");
    }
}

package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Similar to {@link RawDataV1}, but used to hold data from https://ga-covid19.ondemand.sas.com/static/js/main.js, which
 * the DPH has moved to at this point.
 */
@Data
@ToString(exclude = {"payload"})
public class RawDataV2 implements RawData {
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

package org.dshaver.covid.domain;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Similar to {@link RawDataV1}, but used to hold data from https://ga-covid19.ondemand.sas.com/static/js/main.js, which
 * the DPH has moved to at this point.
 */
@Data
@ToString(exclude = {"payload"})
@Document("rawdatav2")
public class RawDataV2 {
    @Id
    private String id;

    private LocalDateTime createTime;

    private LocalDate reportDate;

    private String payload;

    private Integer epicurveStartIndex;

    private Integer epicurveEndIndex;
}

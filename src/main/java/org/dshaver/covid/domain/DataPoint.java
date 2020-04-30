package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by xpdf64 on 2020-04-22.
 */
@Data
public class DataPoint {
    private int y;
    private String label;
    private String source;

    public DataPoint() {
    }

    public DataPoint(int y, String label, String source) {
        this.y = y;
        this.label = label;
        this.source = source;
    }

    @JsonIgnore
    public DataPoint copy(int y) {
        return new DataPoint(y, this.label, this.source);
    }
}

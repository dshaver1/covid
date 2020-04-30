package org.dshaver.covid.domain;

import lombok.Data;

import java.util.List;

/**
 * Created by xpdf64 on 2020-04-22.
 */
@Data
public class Series {
    private List<DataPoint> dataPoints;

    public Series() {
    }

    public Series(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }
}

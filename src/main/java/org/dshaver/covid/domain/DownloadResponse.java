package org.dshaver.covid.domain;

import lombok.Data;

/**
 * Created by xpdf64 on 2020-04-28.
 */
@Data
public class DownloadResponse {
    private boolean foundNew;
    private Report report;
}

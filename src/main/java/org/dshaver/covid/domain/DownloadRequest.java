package org.dshaver.covid.domain;

import lombok.Data;

import java.util.List;

/**
 * Created by xpdf64 on 2020-04-29.
 */
@Data
public class DownloadRequest {
    private List<String> urls;
}

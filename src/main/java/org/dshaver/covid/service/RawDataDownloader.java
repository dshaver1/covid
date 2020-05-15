package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawData;

public interface RawDataDownloader<T extends RawData> {
    T download(String urlString);
}

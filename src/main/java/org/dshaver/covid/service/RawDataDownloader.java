package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawData;

import java.io.InputStream;

public interface RawDataDownloader<T extends RawData> {
    T download(String urlString);

    T transform(InputStream inputStream, boolean writeToDisk);
}

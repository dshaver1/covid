package org.dshaver.covid.service.extractor;

import java.util.List;
import java.util.Optional;

public interface Extractor<T, R> {
    //TODO refactor this to accept RawData
    Optional<R> extract(List<T> raw, String id);
}

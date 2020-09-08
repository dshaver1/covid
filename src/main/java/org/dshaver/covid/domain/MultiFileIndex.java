package org.dshaver.covid.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class MultiFileIndex {
    private LocalDateTime lastUpdated;
    private final Map<Class<? extends Identifiable>, FileIndex> multimap = new HashMap<>();
}

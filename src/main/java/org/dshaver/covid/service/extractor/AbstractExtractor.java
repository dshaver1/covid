package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Pattern;

public abstract class AbstractExtractor {
    private final ObjectMapper objectMapper;

    protected AbstractExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public abstract Pattern getPattern();
}

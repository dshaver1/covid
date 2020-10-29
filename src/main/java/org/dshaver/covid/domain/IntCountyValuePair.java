package org.dshaver.covid.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record IntCountyValuePair(String county, Integer value) { }

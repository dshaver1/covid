package org.dshaver.covid.service;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Data
@ConfigurationProperties(prefix = "covid")
public class CountyService {
    private static final Logger logger = LoggerFactory.getLogger(CountyService.class);
    private Map<String, Boolean> enabledCounties;

    /**
     * Returns true if the query string matches any part of the supplied list, OR if any of the supplied list match part of the query string.
     */
    @Cacheable("enabledCounties")
    public boolean isCountyEnabled(String query) {
        String lowercaseQuery = StringUtils.deleteWhitespace(query.toLowerCase());
        Boolean enabled = getEnabledCounties().get(lowercaseQuery);

        if (enabled == null) {
            logger.info("Could not find county {} in configuration! Searching for partial matches...", query);
            for (String current : getEnabledCounties().keySet()) {
                if (current.contains(lowercaseQuery) || lowercaseQuery.contains(current)) {
                    enabled = getEnabledCounties().get(current);
                    logger.info("Found partial match for county {}: {} with value {}", query, current, enabled);
                    break;
                }
            }
        }

        return enabled == null ? false : enabled;
    }

    public List<String> getAllEnabledCounties() {
        return getEnabledCounties().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}

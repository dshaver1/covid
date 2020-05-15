package org.dshaver.covid.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RawDataParsingTools {
    /**
     * Get the specified pattern (that is assumed to have exactly 1 regex group) from the provided rawData.
     */
    public static Optional<String> getVarFromRegex(List<String> rawData, Pattern pattern) {
        for (String s : rawData) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }

        return Optional.empty();
    }
}

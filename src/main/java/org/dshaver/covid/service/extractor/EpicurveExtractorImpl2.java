package org.dshaver.covid.service.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dshaver.covid.domain.epicurve.*;
import org.dshaver.covid.domain.overview.ReportOverviewImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Pattern;

import static org.dshaver.covid.service.RawDataParsingTools.getVarFromRegex;

@Component
public class EpicurveExtractorImpl2 extends AbstractExtractor implements Extractor<String, Epicurve> {
    private static final Logger logger = LoggerFactory.getLogger(EpicurveExtractorImpl2.class);
    private static final Pattern epicurvePattern = Pattern.compile(".*JSON.parse\\('(\\[\\{\"measure\".+?]'\\)}).*");
    private static final LocalDate EARLIEST_DATE = LocalDate.of(2020, 2, 16);

    @Inject
    protected EpicurveExtractorImpl2(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Optional<Epicurve> extract(List<String> raw, String id) {
        Optional<String> epicurveString = getVarFromRegex(raw, getPattern());
        List<EpicurvePointImpl2> epicurvePoints = null;
        Optional<Epicurve> epicurve = Optional.empty();

        try {
            if (epicurveString.isPresent()) {
                Collection<EpicurvePointImpl2> filteredDataPoints = new TreeSet<>();
                epicurvePoints = getObjectMapper().readValue(epicurveString.get(), new TypeReference<List<EpicurvePointImpl2>>(){});
                for (EpicurvePointImpl2 current : epicurvePoints) {
                    LocalDate labelDate = LocalDate.parse(current.getTestDate(), DateTimeFormatter.ISO_DATE);
                    if (labelDate.isAfter(EARLIEST_DATE)) {
                        current.setSource(id);
                        current.setLabel(labelDate.format(DateTimeFormatter.ISO_DATE).toUpperCase());
                        filteredDataPoints.add(current);
                    }
                }

                epicurve = Optional.of(new EpicurveImpl2(filteredDataPoints));
            }
        } catch (Exception e) {
            logger.info("Could not find epicurve with pattern {}", getPattern());
        }

        return epicurve;
    }

    @Override
    public Pattern getPattern() {
        return epicurvePattern;
    }
}

package org.dshaver.covid.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.dshaver.covid.domain.BasicFile;
import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Downloads website and minimally parses it to pull out the datetime the report was published.
 */
@Component
public class RawDataDownloader2 implements RawDataDownloader<RawDataV2> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataDownloader2.class);
    private final Pattern timePattern = Pattern.compile(".*JSON.parse\\('\\{\"currdate\":\"(\\d{1,2}/\\d{1,2}/\\d{4},\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[AP]M)\"}.*");
    private final Pattern allJsonPattern = Pattern.compile("JSON\\.parse\\('(.*?)'\\)");
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("M/dd/uuuu, hh:mm:ss a").toFormatter();
    private final RawDataWriter rawDataWriter;

    @Inject
    public RawDataDownloader2(RawDataWriter rawDataWriter) {
        this.rawDataWriter = rawDataWriter;
    }

    public RawDataV2 download(String urlString) {
        logger.info("Downloading report from " + urlString);
        try {
            URL url = new URL(urlString);
            try (InputStream inputStream = url.openStream()) {
                RawDataV2 rawData = transform(inputStream, true);

                return rawData;
            } catch (IOException e) {
                logger.error("Error opening stream!");
            }
        } catch (MalformedURLException me) {
            throw new RuntimeException(me);
        }

        return null;
    }

    @Override
    public RawDataV2 transform(InputStream inputStream, boolean writeToDisk) {
        logger.info("Filtering supplied inputStream...");
        RawDataV2 rawData = new RawDataV2();
        rawData.setCreateTime(LocalDateTime.now());

        List<String> downloadedStrings = new ArrayList<>();
        List<String> filteredStrings = new ArrayList<>();
        BufferedReader br;
        String line;
        LocalDateTime dateObj = null;

        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                downloadedStrings.add(StringUtils.deleteWhitespace(line));
                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.matches()) {
                    String dateTimeString = timeMatcher.group(1);
                    dateObj = LocalDateTime.from(timeFormatter.parse(dateTimeString));
                    rawData.setId(dateObj.format(DateTimeFormatter.ISO_DATE_TIME));
                    rawData.setReportDate(dateObj.toLocalDate());
                }

                Matcher jsonMatcher = allJsonPattern.matcher(line);
                while (jsonMatcher.find()) {
                    filteredStrings.add(jsonMatcher.group(1));
                }
            }

        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (dateObj == null) {
            throw new IllegalStateException("Could not find date in raw DPH data!");
        }

        List<String> payload = new ArrayList<>();
        payload.add(String.join("", downloadedStrings));

        if (writeToDisk) {
            rawDataWriter.write(new BasicFile(rawData.getReportDate(), rawData.getId().replace(":",""), null, payload));
        }

        rawData.setPayload(filteredStrings);
        return rawData;
    }
}

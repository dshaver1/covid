package org.dshaver.covid.service;

import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.dao.RawDataRepositoryV3;
import org.dshaver.covid.domain.RawDataV3;
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

/**
 * Downloads website and minimally parses it to pull out the datetime the report was published.
 */
@Component
public class RawDataDownloader3 implements RawDataDownloader<RawDataV3> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataDownloader3.class);
    private final Pattern timePattern = Pattern.compile(".*JSON.parse\\('\\{\"currdate\":\"(\\d{1,2}/\\d{1,2}/\\d{4},\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[AP]M)\"}.*");
    private final Pattern allJsonPattern = Pattern.compile("JSON\\.parse\\('(.*?)'\\)");
    private final Pattern mainNoncePattern = Pattern.compile("main\\.(\\w*)\\.chunk\\.js");
    private final Pattern fourthChunkNoncePattern = Pattern.compile("\\{3:\"\\w*\",4:\"(\\w*)\"}");
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("M/dd/uuuu, hh:mm:ss a").toFormatter();
    private final RawDataRepositoryV3 rawDataWriter;

    @Inject
    public RawDataDownloader3(RawDataRepositoryV3 rawDataWriter) {
        this.rawDataWriter = rawDataWriter;
    }

    public RawDataV3 download(String urlString) {
        logger.info("Downloading report from " + urlString);
        String mainNonce = "";
        String fourthChunkNonce = "";
        List<String> indexStrings = new ArrayList<>();
        // Find chunk urls
        try {
            URL url = new URL(urlString);
            try (InputStream inputStream = url.openStream()) {
                BufferedReader br;
                String line;
                br = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = br.readLine()) != null) {
                    indexStrings.add(line);
                    Matcher fourthChunkNonceMatcher = fourthChunkNoncePattern.matcher(line);
                    // Should only be 1 match... Gotta find before group.
                    fourthChunkNonceMatcher.find();
                    fourthChunkNonce = fourthChunkNonceMatcher.group(1);

                    Matcher mainNonceMatcher = mainNoncePattern.matcher(line);
                    // Should only be 1 match... Gotta find before group.
                    mainNonceMatcher.find();
                    mainNonce = mainNonceMatcher.group(1);
                }
            } catch (IOException e) {
                logger.error("Error opening stream!");
            }
        } catch (MalformedURLException me) {
            throw new RuntimeException(me);
        }

        try {
            String mainUrlString = buildUrl(urlString, "main", mainNonce);
            URL mainUrl = new URL(mainUrlString);
            String fourthChunkUrlString = buildUrl(urlString, "4", fourthChunkNonce);
            URL fourthChunkUrl = new URL(fourthChunkUrlString);

            logger.info("Downloading from {}...", mainUrl);
            RawDataV3 rawDataMain = new RawDataV3();
            try (InputStream inputStream = mainUrl.openStream()) {
                rawDataMain = transform(inputStream, true);
            } catch (IOException e) {
                logger.error("Error opening stream!");
            }

            logger.info("Downloading from {}...", mainUrl);
            RawDataV3 rawDataFourth = new RawDataV3();
            try (InputStream inputStream = mainUrl.openStream()) {
                rawDataFourth = transform(inputStream, true);
            } catch (IOException e) {
                logger.error("Error opening stream!");
            }

            RawDataV3 rawData = new RawDataV3();
            rawData.setCreateTime(rawDataMain.getCreateTime());
            rawData.setId(rawDataMain.getId());
            rawData.setReportDate(rawDataMain.getReportDate());
            List<String> payload = indexStrings;
            payload.addAll(rawDataMain.getPayload());
            payload.addAll(rawDataFourth.getPayload());
            rawData.setPayload(payload);

            try {
                rawDataWriter.save(rawData);
            } catch (IOException e) {
                throw new RuntimeException("Error saving RawDataV3!", e);
            }

            return rawData;
        } catch (MalformedURLException me) {
            throw new RuntimeException(me);
        }
    }

    @Override
    public RawDataV3 transform(InputStream inputStream, boolean writeToDisk) {
        logger.info("Filtering supplied inputStream...");
        RawDataV3 rawData = new RawDataV3();
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

        rawData.setPayload(filteredStrings);
        return rawData;
    }

    private String buildUrl(String baseUrl, String prefix, String nonce) {
        return baseUrl + "static/js/" + prefix + "." + nonce + ".chunk.js";
    }
}

package org.dshaver.covid.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dshaver.covid.dao.RawDataRepositoryV3;
import org.dshaver.covid.domain.RawDataV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Pattern numberedChunkNoncePattern = Pattern.compile("\"static/js/\".+(\\{(\\d{1,2}:\"[\\d|\\w]+\",?)+})");
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("M/dd/uuuu, hh:mm:ss a").toFormatter();
    private final RawDataRepositoryV3 rawDataWriter;
    private final ObjectMapper objectMapper;

    @Inject
    public RawDataDownloader3(RawDataRepositoryV3 rawDataWriter, ObjectMapper objectMapper) {
        this.rawDataWriter = rawDataWriter;
        this.objectMapper = objectMapper;
    }

    public RawDataV3 download(String urlString) {
        logger.info("Downloading report from " + urlString);
        String mainNonce = "";
        String numberedChunkString = "";
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
                    Matcher numberedChunkNonceMatcher = numberedChunkNoncePattern.matcher(line);
                    // Should only be 1 match... Gotta find before group.
                    if (numberedChunkNonceMatcher.find()) {
                        numberedChunkString = numberedChunkNonceMatcher.group(1);
                    }

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

            logger.info("Downloading from {}...", mainUrl);
            RawDataV3 rawDataMain = new RawDataV3();
            try (InputStream inputStream = mainUrl.openStream()) {
                rawDataMain = transform(inputStream, true);
            } catch (IOException e) {
                logger.error("Error opening stream!");
            }

            RawDataV3 rawData = new RawDataV3();
            rawData.setCreateTime(rawDataMain.getCreateTime());
            rawData.setId(rawDataMain.getId());
            rawData.setReportDate(rawDataMain.getReportDate());
            List<String> payload = indexStrings;
            payload.addAll(rawDataMain.getPayload());

            if (StringUtils.isNotEmpty(numberedChunkString)) {
                logger.info("Downloading all numbered chunks using nonce map... {}", numberedChunkString);

                List<String> numberedDownloads = downloadAllChunks(urlString, numberedChunkString);

                payload.addAll(numberedDownloads);
            }

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

    private List<String> downloadAllChunks(String urlString, String numberedChunkString) throws MalformedURLException {
        List<String> accumulator = new ArrayList<>();

        Map<Integer, String> nonceMap = parseChunkMap(numberedChunkString);

        for (Map.Entry<Integer, String> entry : nonceMap.entrySet()) {
            String currentChunkUrlString = buildUrl(urlString, entry.getKey().toString(), entry.getValue());
            logger.info("Downloading chunk from {}...", currentChunkUrlString);
            URL currentChunkUrl = new URL(currentChunkUrlString);

            RawDataV3 currentRawData = new RawDataV3();
            try (InputStream inputStream = currentChunkUrl.openStream()) {
                currentRawData = transform(inputStream, true);
            } catch (IOException e) {
                logger.error("Error opening stream! for chunk: " + entry.getKey() + " with nonce: " + entry.getValue(), e );
            }

            accumulator.addAll(currentRawData.getPayload());
        }

        return accumulator;
    }

    private Map<Integer, String> parseChunkMap(String numberedChunkString) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructMapType(HashMap.class, Integer.class, String.class);
            return objectMapper.readValue(numberedChunkString, type);
        } catch (Exception e) {
            logger.error("Could not read nonce map! " + numberedChunkString, e);
        }

        return null;
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
            logger.error("Could not find date in raw DPH data!");
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

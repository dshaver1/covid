package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawDataV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
public class RawDataDownloader2 {
    private static final Logger logger = LoggerFactory.getLogger(RawDataDownloader2.class);
    private final Pattern timePattern = Pattern.compile(".*JSON.parse\\('\\{\"currdate\":\"(\\d{1,2}/\\d{1,2}/\\d{4},\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[AP]M)\"}.*");
    private final DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().appendPattern("M/dd/uuuu, hh:mm:ss a").toFormatter();


    public RawDataV2 download(String urlString) {
        logger.info("Downloading report from " + urlString);
        RawDataV2 rawData = new RawDataV2();
        rawData.setCreateTime(LocalDateTime.now());

        List<String> downloadedStrings = new ArrayList<>();
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        try {
            url = new URL(urlString);
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                downloadedStrings.add(line);
                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.matches()) {
                    String dateTimeString = timeMatcher.group(1);
                    LocalDateTime dateObj = LocalDateTime.from(timeFormatter.parse(dateTimeString));
                    rawData.setId(dateObj.format(DateTimeFormatter.ISO_DATE_TIME));
                    rawData.setReportDate(dateObj.toLocalDate());
                }
            }

        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }

        rawData.setPayload(downloadedStrings);
        return rawData;
    }
}

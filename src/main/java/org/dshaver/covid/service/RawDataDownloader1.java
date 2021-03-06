package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawDataV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads website and minimally parses it to pull out the datetime the report was published.
 */
@Component
public class RawDataDownloader1 implements RawDataDownloader<RawDataV1> {
    private static final Logger logger = LoggerFactory.getLogger(RawDataDownloader1.class);
    private final Pattern timePattern = Pattern.compile(".*Public Health as of (\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}).*");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss");


    public RawDataV1 download(String urlString) {
        logger.info("Downloading report from " + urlString);
        RawDataV1 rawData = new RawDataV1();
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
                Matcher matcher = timePattern.matcher(line);
                if (matcher.matches()) {
                    String dateTimeString = matcher.group(1);
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

    // TODO
    @Override
    public RawDataV1 transform(InputStream inputStream, boolean writeToDisk) {
        return null;
    }
}

package org.dshaver.covid.service;

import org.dshaver.covid.domain.RawData;
import org.dshaver.covid.domain.RawDataV1;
import org.dshaver.covid.domain.RawDataV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class RawDataDownloaderDelegator {
    private final RawDataDownloader<RawDataV1> downloaderV1;
    private final RawDataDownloader<RawDataV2> downloaderV2;
    private final String downloadUrl1;
    private final String downloadUrl2;

    @Inject
    public RawDataDownloaderDelegator(RawDataDownloader<RawDataV1> downloaderV1,
                                      RawDataDownloader<RawDataV2> downloaderV2,
                                      @Value("${covid.download.url}") String downloadUrl1,
                                      @Value("${covid.download.url2}") String downloadUrl2) {
        this.downloaderV1 = downloaderV1;
        this.downloaderV2 = downloaderV2;
        this.downloadUrl1 = downloadUrl1;
        this.downloadUrl2 = downloadUrl2;
    }

    public RawData download(Class<? extends RawData> rawDataClass) {
        if (rawDataClass.equals(RawDataV2.class)) {
            return downloaderV2.download(downloadUrl2);
        }

        return downloaderV1.download(downloadUrl1);
    }

    /**
     * Wrote this to do the one-time download from internet archives.
     */
    public void bulkDownload() {
        List<String> urls = new ArrayList<>();
        //6PM reports
        urls.add("https://web.archive.org/web/20200401233102/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200402233057/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200403233059/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200404233057/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200406235830/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200407233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200408233107/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200409233108/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200410233107/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200411233109/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200412233116/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200413233106/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200414233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200415233104/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200416233056/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200417233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200418233059/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200420233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200421233835/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200422233103/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200423233105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200424233102/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200425233100/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200426233107/https://d20s4vd27d0hk0.cloudfront.net/");

        //12PM reports
        urls.add("https://web.archive.org/web/20200328113218/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200331185130/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200401155500/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200402130332/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200403161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200404161034/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200405181443/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200406161114/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200407161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200408161036/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200409162105/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200410161045/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200411161053/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200412162041/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200413161051/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200414161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200415161043/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200416161055/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200417161039/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200418161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200419161040/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200420161039/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200421161041/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200422161044/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200423161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200424161044/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200425161043/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200426162519/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200427161042/https://d20s4vd27d0hk0.cloudfront.net/");
        urls.add("https://web.archive.org/web/20200428170749/https://d20s4vd27d0hk0.cloudfront.net/");

        urls.forEach(downloaderV1::download);
    }
}

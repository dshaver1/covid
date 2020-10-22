package org.dshaver.covid.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.dshaver.covid.domain.*;
import org.dshaver.covid.service.*;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl1;
import org.dshaver.covid.service.extractor.EpicurveExtractorImpl2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.dshaver.covid.dao.BaseFileRepository.idFormatter;
import static org.dshaver.covid.service.CsvService.cleanCounty;

/**
 * Created by xpdf64 on 2020-06-12.
 */
@RestController
public class RawDataController {
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataWriter rawDataWriter;
    private final RawDataFileRepository fileRepository;
    private final EpicurveExtractorImpl1 extractorImpl1;
    private final EpicurveExtractorImpl2 extractorImpl2;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;
    private final FileRegistry fileRegistry;
    private final RawDataDownloaderDelegator rawDataDownloader;
    private final CsvService csvService;

    @Inject
    public RawDataController(RawDataWriter rawDataWriter,
                             RawDataFileRepository fileRepository,
                             EpicurveExtractorImpl1 extractorImpl1,
                             EpicurveExtractorImpl2 extractorImpl2,
                             ObjectMapper objectMapper,
                             ReportService reportService,
                             FileRegistry fileRegistry,
                             RawDataDownloaderDelegator rawDataDownloader,
                             CsvService csvService) {
        this.rawDataWriter = rawDataWriter;
        this.fileRepository = fileRepository;
        this.extractorImpl1 = extractorImpl1;
        this.extractorImpl2 = extractorImpl2;
        this.objectMapper = objectMapper;
        this.reportService = reportService;
        this.fileRegistry = fileRegistry;
        this.rawDataDownloader = rawDataDownloader;
        this.csvService = csvService;
    }

    @PostMapping("/covid/api/poll")
    public DownloadResponse checkForData(@RequestParam(name = "force", required = false) Boolean force) throws Exception {
        boolean defaultedForce = force == null ? false : force;
        return reportService.checkForData(defaultedForce);
    }

    @PostMapping("/covid/api/download")
    public void downloadFromUrl(@RequestParam(name = "url", required = false) String url) throws Exception {
        rawDataDownloader.download(url);
    }

    @PostMapping("/covid/api/reprocess")
    public void reprocess(@RequestParam(name = "startDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(name = "endDate", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                          @RequestParam(name = "clean", required = false) Boolean clean,
                          @RequestParam(name = "saveIntermediate", required = false) Boolean saveIntermediate) throws Exception {
        LocalDate defaultedStartDate = startDate == null ? LocalDate.of(2020, 1, 1) : startDate.minusDays(1);
        LocalDate defaultedEndDate = endDate == null ? getDefaultEndDate() : endDate.plusDays(1);
        boolean defaultedClean = clean == null ? true : clean;
        boolean defaultedSaveIntermediate = saveIntermediate == null ? true : saveIntermediate;

        reportService.processRange(defaultedStartDate, defaultedEndDate, defaultedClean, defaultedSaveIntermediate);

        logger.info("Done reprocessing {} to {}!", defaultedStartDate, defaultedEndDate);
    }

    /**
     * Use today if it's after 3pm, but use yesterday if it's before 3pm.
     */
    private LocalDate getDefaultEndDate() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() >= 15) {
            return now.toLocalDate();
        }

        return now.toLocalDate().minusDays(1);
    }

    @PostMapping("/covid/api/transformRaw")
    public void transformRaw() throws Exception {
        String v1PathString = "H:\\dev\\workingdir\\covid\\raw\\temp";
        String v2PathString = "H:\\dev\\workingdir\\covid\\raw\\v2";
        Path outputDir = Paths.get("H:\\dev\\workingdir\\covid\\raw\\v0");
        List<Path> paths = Streams.concat(Files.list(Paths.get(v1PathString)))
                //, Files.list(Paths.get(v2PathString)))
                .collect(Collectors.toList());

        for (Path path : paths) {
            String originalFilename = path.getFileName().toString();
            String outputFilename = originalFilename.substring(0, originalFilename.indexOf("."));
            Path fullOutputPath = outputDir.resolve(outputFilename + ".json");

            List<String> allLines = Files.readAllLines(path);
            try (BufferedWriter writer = Files.newBufferedWriter(fullOutputPath)) {
                String id = outputFilename.substring(outputFilename.indexOf("2"));

                LocalDate reportDate = LocalDateTime.parse(id, idFormatter).toLocalDate();

                BasicFile file = new BasicFile(reportDate, id, fullOutputPath, allLines);

                objectMapper.writeValue(writer, file);
            }
        }
    }

    @PostMapping("/covid/api/scanDirectories")
    public void scan() throws Exception {
        fileRegistry.checkAndSaveIndex();
    }

    @GetMapping("/covid/api/download/{county}")
    public ResponseEntity<Resource> downloadCountyZip(@PathVariable("county") String county) throws ArchiveException, IOException {
        String cleanedCounty = cleanCounty(county);
        logger.info("Got request to download zip of {} data!", cleanedCounty);
        Optional<String> maybeLatestId = fileRegistry.getLatestId(Report.class);

        if (!maybeLatestId.isPresent()) {
            throw new IllegalStateException("No reports in the system? Bad news.");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, outputStream);

        Map<String, String> fileMap = Files.list(csvService.getCountyDirPath(county))
                .collect(Collectors.toMap(p -> p.getFileName().toString(), this::safeReadFile));

        ZipService.writeZip(fileMap, archive);
        archive.finish();
        byte[] bytes = outputStream.toByteArray();
        ByteArrayResource byteArrayResource = new ByteArrayResource(bytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("content-disposition", "attachment; filename=" + cleanedCounty + "-epicurve-data-" + maybeLatestId.get() + ".zip");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(bytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(byteArrayResource);
    }

    @GetMapping("health")
    public String healthCheck() {
        return "UP";
    }

    private String getExtension(RawData data) {
        if (data instanceof RawDataV1) {
            return ".html";
        } else {
            return ".js";
        }
    }

    private String safeReadFile(Path path) {
        String fileContents = "";

        try {
            fileContents = String.join("\n", Files.readAllLines(path));
        } catch (IOException e) {
            logger.error("Could not read " + path);
        }

        return fileContents;
    }
}

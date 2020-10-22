package org.dshaver.covid.service;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class ZipService {
    public static void writeZip(Map<String, String> files, ArchiveOutputStream archive) throws IOException {
        for (Map.Entry<String, String> file : files.entrySet()) {
            ZipArchiveEntry entry = new ZipArchiveEntry(file.getKey());

            byte[] contents = file.getValue().getBytes(Charset.forName("UTF-8"));

            entry.setSize(contents.length);
            archive.putArchiveEntry(entry);
            archive.write(contents);
            archive.closeArchiveEntry();
        }
    }
}

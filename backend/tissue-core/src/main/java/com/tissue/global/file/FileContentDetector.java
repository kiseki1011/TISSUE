package com.tissue.global.file;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class FileContentDetector {

    private static final int HEADER_SIZE = 65536;

    private final Tika tika = new Tika();

    public String detect(InputStream inputStream, String filename) throws IOException {
        byte[] header = inputStream.readNBytes(HEADER_SIZE);
        return tika.detect(header, filename);
    }
}

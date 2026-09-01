package com.beno.summaryspherebackend.services.impl;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.apache.tika.Tika;
import com.beno.summaryspherebackend.services.FileExtractionService;

@Service
public class FileExtractionServiceImpl implements FileExtractionService {

    private static final int MAX_EXTRACTED_CHARACTERS = 1_000_000;

    private final Tika tika;

    public FileExtractionServiceImpl() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_EXTRACTED_CHARACTERS + 1);
    }

    @Override
    public String extractText(InputStream inputStream) throws Exception {
        String text = tika.parseToString(inputStream);

        if (text.length() > MAX_EXTRACTED_CHARACTERS) {
            throw new IllegalArgumentException(
                    "Document contains too much text. Maximum: "
                    + MAX_EXTRACTED_CHARACTERS + " characters"
            );
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException(
                    "No readable text could be extracted from the document"
            );
        }

        return text;
    }
}
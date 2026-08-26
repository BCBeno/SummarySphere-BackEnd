package com.beno.summaryspherebackend.services.impl;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.apache.tika.Tika;
import com.beno.summaryspherebackend.services.FileExtractionService;

@Service
public class FileExtractionServiceImpl implements FileExtractionService {

    private final Tika tika = new Tika();

    @Override
    public String extractText(InputStream inputStream) throws Exception {
        return tika.parseToString(inputStream);
    }
}
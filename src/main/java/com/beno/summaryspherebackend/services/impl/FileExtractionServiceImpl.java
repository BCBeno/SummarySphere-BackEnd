package com.beno.summaryspherebackend.services.impl;
import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;
import org.apache.tika.Tika;
import com.beno.summaryspherebackend.services.FileExtractionService;

@Service
public class FileExtractionServiceImpl implements FileExtractionService {
    @Override
    public String extractTextFromBytes(byte[] bytes) throws Exception {
        Tika tika = new Tika();
        return tika.parseToString(new ByteArrayInputStream(bytes));
    }
}

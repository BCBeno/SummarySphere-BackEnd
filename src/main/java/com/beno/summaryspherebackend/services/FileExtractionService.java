package com.beno.summaryspherebackend.services;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@Service
public class FileExtractionService {
    public String extractTextFromBytes(byte[] bytes) throws Exception {
        Tika tika = new Tika();
        return tika.parseToString(new ByteArrayInputStream(bytes));
    }
}

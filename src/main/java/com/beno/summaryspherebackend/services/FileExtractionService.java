package com.beno.summaryspherebackend.services;

import java.io.InputStream;

public interface FileExtractionService {

    String extractText(InputStream inputStream) throws Exception;
}
package com.beno.summaryspherebackend.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {
    void upload(String key, InputStream input, long contentLength, String contentType) throws IOException;

    boolean exists(String key);

    byte[] download(String key);

    void delete(String key);

    String createPresignedDownloadUrl(String key, Duration expiration);
}

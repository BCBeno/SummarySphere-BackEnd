package com.beno.summaryspherebackend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class S3StorageConfigTest {
    @Test
    void configuresStaticCredentialsAndBuildsClientsWithoutCheckingBucket() {
        S3StorageConfig config = new S3StorageConfig();
        StaticCredentialsProvider credentials = config.s3CredentialsProvider("test-access-key", "test-secret-key");

        assertEquals("test-access-key", credentials.resolveCredentials().accessKeyId());
        try (var client = config.s3Client("https://storage.example.test", "us-east-1", credentials);
             var presigner = config.s3Presigner("https://storage.example.test", "us-east-1", credentials)) {
            assertNotNull(client);
            assertNotNull(presigner);
        }
    }
}

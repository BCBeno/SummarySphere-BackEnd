package com.beno.summaryspherebackend.services.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.beno.summaryspherebackend.exceptions.ObjectStorageException;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3ObjectStorageServiceTest {
    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private final S3ObjectStorageService storage = new S3ObjectStorageService(s3Client, presigner, "test-bucket");

    @Test
    void uploadSendsTheConfiguredBucketKeyAndContentType() throws Exception {
        storage.upload("documents/doc.pdf", new ByteArrayInputStream(new byte[]{1, 2}), 2,
                "application/pdf");

        var request = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals("documents/doc.pdf", request.getValue().key());
        assertEquals("application/pdf", request.getValue().contentType());
    }

    @Test
    void downloadReturnsObjectBytes() {
        byte[] expected = "stored text".getBytes();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), expected));

        assertArrayEquals(expected, storage.download("documents/doc/content.txt"));
    }

    @Test
    void deleteUsesTheConfiguredBucketAndKey() {
        storage.delete("document.pdf");

        var request = org.mockito.ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals("document.pdf", request.getValue().key());
    }

    @Test
    void existsReturnsTrueWhenHeadRequestSucceeds() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(software.amazon.awssdk.services.s3.model.HeadObjectResponse.builder().build());

        assertTrue(storage.exists("document.pdf"));
    }

    @Test
    void existsReturnsFalseWhenObjectIsMissing() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("missing").build());

        assertFalse(storage.exists("missing.pdf"));
    }

    @Test
    void storageErrorsBecomeSafeApplicationExceptions() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("upstream failure").build());

        ObjectStorageException error = assertThrows(ObjectStorageException.class,
                () -> storage.delete("document.pdf"));
        assertEquals("Unable to delete the object in S3 storage.", error.getMessage());
    }

    @Test
    void networkErrorsBecomeSafeApplicationExceptions() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("network unavailable"));

        ObjectStorageException error = assertThrows(ObjectStorageException.class,
                () -> storage.delete("document.pdf"));
        assertEquals("Unable to delete the object in S3 storage.", error.getMessage());
    }

    @Test
    void presignedGetUrlUsesRequestedExpirationAndReturnsTheUrl() throws Exception {
        PresignedGetObjectRequest signedRequest = mock(PresignedGetObjectRequest.class);
        when(signedRequest.url()).thenReturn(new java.net.URL("https://storage.example.test/test-bucket/doc.pdf?X-Amz-Signature=temporary"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signedRequest);

        String url = storage.createPresignedDownloadUrl("doc.pdf", Duration.ofMinutes(10));

        assertTrue(url.startsWith("https://storage.example.test/test-bucket/doc.pdf?"));
        var request = org.mockito.ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertEquals(Duration.ofMinutes(10), request.getValue().signatureDuration());
        assertEquals("test-bucket", request.getValue().getObjectRequest().bucket());
        assertEquals("doc.pdf", request.getValue().getObjectRequest().key());
    }
}

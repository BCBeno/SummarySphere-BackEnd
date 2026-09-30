package com.beno.summaryspherebackend.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.beno.summaryspherebackend.exceptions.ObjectStorageException;
import com.beno.summaryspherebackend.services.ObjectStorageService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class S3ObjectStorageService implements ObjectStorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3ObjectStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    @Override
    public void upload(String key, InputStream input, long contentLength, String contentType) throws IOException {
        PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key);
        if (contentType != null && !contentType.isBlank()) {
            request.contentType(contentType);
        }
        try {
            s3Client.putObject(request.build(), RequestBody.fromInputStream(input, contentLength));
        } catch (S3Exception ex) {
            throw storageFailure("upload", ex);
        } catch (SdkException ex) {
            throw storageFailure("upload", ex);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw storageFailure("check object", ex);
        } catch (SdkException ex) {
            throw storageFailure("check object", ex);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
                    .asByteArray();
        } catch (S3Exception ex) {
            throw storageFailure("download", ex);
        } catch (SdkException ex) {
            throw storageFailure("download", ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            throw storageFailure("delete", ex);
        } catch (SdkException ex) {
            throw storageFailure("delete", ex);
        }
    }

    @Override
    public String createPresignedDownloadUrl(String key, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .responseContentDisposition("attachment")
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest signedRequest = s3Presigner.presignGetObject(presignRequest);
            return signedRequest.url().toString();
        } catch (S3Exception ex) {
            throw storageFailure("create download link", ex);
        } catch (SdkException ex) {
            throw storageFailure("create download link", ex);
        }
    }

    private ObjectStorageException storageFailure(String operation, Throwable cause) {
        return new ObjectStorageException("Unable to " + operation + " the object in S3 storage.", cause);
    }
}

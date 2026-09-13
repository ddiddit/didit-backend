package com.didit.adapter.integration.storage

import com.didit.application.retrospect.required.AttachmentStorage
import com.didit.application.retrospect.required.StoredAttachmentMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

@Component
class S3AttachmentStorage(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @param:Value("\${retrospective.attachments.bucket}") private val bucket: String,
) : AttachmentStorage {
    override fun createUploadUrl(
        storageKey: String,
        contentType: String,
        contentLength: Long,
        checksumSha256: String,
        validity: Duration,
    ): URI {
        val objectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .checksumSHA256(checksumSha256)
                .build()
        return s3Presigner
            .presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(validity)
                    .putObjectRequest(objectRequest)
                    .build(),
            ).url()
            .toURI()
    }

    override fun inspect(storageKey: String): StoredAttachmentMetadata {
        val head =
            s3Client.headObject(
                HeadObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build(),
            )
        val firstBytes =
            s3Client
                .getObjectAsBytes(
                    GetObjectRequest
                        .builder()
                        .bucket(bucket)
                        .key(storageKey)
                        .range("bytes=0-4095")
                        .build(),
                ).asByteArray()
        return StoredAttachmentMetadata(
            contentLength = head.contentLength(),
            contentType = head.contentType() ?: "application/octet-stream",
            firstBytes = firstBytes,
        )
    }

    override fun read(storageKey: String): ByteArray =
        s3Client
            .getObjectAsBytes(
                GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build(),
            ).asByteArray()

    override fun createDownloadUrl(
        storageKey: String,
        validity: Duration,
    ): URI =
        s3Presigner
            .presignGetObject(
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(validity)
                    .getObjectRequest(
                        GetObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .build(),
                    ).build(),
            ).url()
            .toURI()

    override fun delete(storageKey: String) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key(storageKey)
                .build(),
        )
    }
}

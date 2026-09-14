package com.didit.application.retrospect.required

import java.net.URI
import java.time.Duration

interface AttachmentStorage {
    fun createUploadUrl(
        storageKey: String,
        contentType: String,
        contentLength: Long,
        checksumSha256: String,
        validity: Duration,
    ): URI

    fun inspect(storageKey: String): StoredAttachmentMetadata

    fun read(storageKey: String): ByteArray

    fun createDownloadUrl(
        storageKey: String,
        validity: Duration,
    ): URI

    fun delete(storageKey: String)
}

data class StoredAttachmentMetadata(
    val contentLength: Long,
    val contentType: String,
    val firstBytes: ByteArray,
)

package com.didit.adapter.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class AttachmentStorageConfig {
    @Bean
    fun attachmentS3Client(
        @Value("\${retrospective.attachments.region:ap-northeast-2}") region: String,
    ): S3Client =
        S3Client
            .builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build()

    @Bean
    fun attachmentS3Presigner(
        @Value("\${retrospective.attachments.region:ap-northeast-2}") region: String,
    ): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
}

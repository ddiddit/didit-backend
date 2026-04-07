package com.didit.adapter.integration.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FirebaseConfig(
    @param:Value("\${firebase.service-account-path}")
    private val serviceAccountPath: String,
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isNotEmpty()) {
            return FirebaseApp.getInstance()
        }

        val credentials =
            FileInputStream(serviceAccountPath).use {
                GoogleCredentials
                    .fromStream(it)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            }

        val options =
            FirebaseOptions
                .builder()
                .setCredentials(credentials)
                .build()

        return FirebaseApp.initializeApp(options)
    }
}

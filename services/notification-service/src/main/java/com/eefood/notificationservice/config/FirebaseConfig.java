package com.eefood.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

  @Value("${firebase.service-account-key:serviceAccountKey.json}")
  private String serviceAccountKeyPath;

  @PostConstruct
  public void init() {
    try {
      InputStream serviceAccount = getServiceAccountStream();

      if (serviceAccount == null) {
        throw new IllegalStateException("Firebase serviceAccountKey.json not found!");
      }

      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

      FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(credentials)
        .build();

      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options);
        log.info("Firebase initialized successfully!");
      }

      serviceAccount.close();
    } catch (IOException e) {
      log.error("Error initializing Firebase: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize Firebase", e);
    }
  }

  private InputStream getServiceAccountStream() throws IOException {
    // Thử 1: Đọc từ external file (Docker/VPS)
    try {
      Resource externalResource = new FileSystemResource(serviceAccountKeyPath);
      if (externalResource.exists()) {
        log.info("Loading Firebase config from external path: {}", serviceAccountKeyPath);
        return externalResource.getInputStream();
      }
    } catch (Exception e) {
      log.debug("External file not found: {}", serviceAccountKeyPath);
    }

    // Thử 2: Đọc từ /app/config/ (Docker mount point)
    try {
      String dockerPath = "/app/config/serviceAccountKey.json";
      Resource dockerResource = new FileSystemResource(dockerPath);
      if (dockerResource.exists()) {
        log.info("Loading Firebase config from Docker path: {}", dockerPath);
        return dockerResource.getInputStream();
      }
    } catch (Exception e) {
      log.debug("Docker config path not found");
    }

    // Thử 3: Đọc từ classpath (Local development)
    try {
      Resource classpathResource = new ClassPathResource(serviceAccountKeyPath);
      if (classpathResource.exists()) {
        log.info("Loading Firebase config from classpath: {}", serviceAccountKeyPath);
        return classpathResource.getInputStream();
      }
    } catch (Exception e) {
      log.debug("Classpath resource not found: {}", serviceAccountKeyPath);
    }

    log.error("Firebase service account key not found in any location!");
    return null;
  }
}
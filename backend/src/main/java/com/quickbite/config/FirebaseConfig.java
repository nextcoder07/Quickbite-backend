package com.quickbite.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final Environment environment;

    @Value("${quickbite.firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${quickbite.firebase.config-path:}")
    private String configPath;

    @Value("${quickbite.firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${quickbite.firebase.project-id:}")
    private String projectId;

    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void initFirebase() {
        if (!firebaseEnabled) {
            boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");
            if (!isTestProfile) {
                throw new IllegalStateException(
                        "quickbite.firebase.enabled=false is only allowed under the 'test' Spring profile.");
            }
            log.info("Firebase Admin SDK disabled (test profile).");
            return;
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_PROJECT_ID is required when Firebase authentication is enabled.");
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = openServiceAccountStream();
                if (serviceAccount == null) {
                    throw new IllegalStateException(
                            "Firebase Admin credentials missing. Please set FIREBASE_SERVICE_ACCOUNT_JSON in .env/environment or provide a valid FIREBASE_CONFIG_PATH.");
                }

                try (InputStream stream = serviceAccount) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(stream))
                            .setProjectId(projectId)
                            .build();
                    FirebaseApp.initializeApp(options);
                }
                log.info("Firebase Admin SDK initialized successfully for project: {}", projectId);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize Firebase Admin SDK: " + e.getMessage(), e);
        }
    }

    private InputStream openServiceAccountStream() throws Exception {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            String trimmed = serviceAccountJson.trim();
            if (trimmed.startsWith("{")) {
                return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
            }
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            return new ByteArrayInputStream(decoded);
        }

        if (configPath != null && !configPath.isBlank()) {
            File file = new File(configPath);
            if (!file.exists()) {
                file = new File("backend", configPath);
            }
            if (!file.exists()) {
                file = new File("..", configPath);
            }
            if (file.exists()) {
                return new FileInputStream(file);
            }
            return getClass().getClassLoader().getResourceAsStream(configPath);
        }

        return null;
    }
}

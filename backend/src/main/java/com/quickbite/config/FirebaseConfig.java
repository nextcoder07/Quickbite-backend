package com.quickbite.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${quickbite.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${quickbite.firebase.config-path:firebase-adminsdk.json}")
    private String configPath;

    @Value("${quickbite.firebase.project-id:quickbite-campus}")
    private String projectId;

    @PostConstruct
    public void initFirebase() {
        if (!firebaseEnabled) {
            log.info("Firebase Auth verification is running in dev/offline mode. (Set quickbite.firebase.enabled=true with service account json to enable cloud verification)");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;
                File file = new File(configPath);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                } else {
                    serviceAccount = getClass().getClassLoader().getResourceAsStream(configPath);
                }

                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId(projectId)
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK initialized successfully for project: {}", projectId);
                } else {
                    log.warn("Firebase config file not found at: {}. Running in fallback dev mode.", configPath);
                }
            }
        } catch (Exception e) {
            log.error("Could not initialize Firebase Admin SDK: {}. Using fallback dev mode.", e.getMessage());
        }
    }
}

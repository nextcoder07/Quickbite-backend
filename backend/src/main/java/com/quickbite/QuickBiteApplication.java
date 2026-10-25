package com.quickbite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class QuickBiteApplication {
    private static final Logger log = LoggerFactory.getLogger(QuickBiteApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(QuickBiteApplication.class, args);
    }

    private static void loadDotEnv() {
        File[] candidates = new File[] {
                new File(".env"),
                new File("../.env")
        };
        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String val = line.substring(eqIdx + 1).trim();

                        // Strip inline comments if unquoted
                        if (!val.startsWith("\"") && !val.startsWith("'") && val.contains("#")) {
                            val = val.substring(0, val.indexOf('#')).trim();
                        } else if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1).trim();
                        }
                        // Skip template placeholders to preserve local dev fallbacks
                        if (val.contains("<") || val.startsWith("your_") || val.startsWith("rzp_test_YourKeyIdHere")) {
                            continue;
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                            count++;
                        }
                    }
                    log.info("Loaded {} environment properties from {}", count, file.getAbsolutePath());
                    break;
                } catch (Exception ex) {
                    log.warn("Failed reading .env file: {}", ex.getMessage());
                }
            }
        }
    }
}

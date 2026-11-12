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
        configureNetworkAndProxy();
        resolveDatabaseTarget();
        SpringApplication.run(QuickBiteApplication.class, args);
    }

    private static void loadDotEnv() {
        File[] candidates = new File[] {
                new File("backend/.env"),
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
                        // Skip unresolved template placeholder values in .env
                        if (val.contains("<") || val.startsWith("your_") || val.startsWith("rzp_test_YourKeyIdHere")) {
                            continue;
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                            count++;
                        }
                    }
                    log.info("Loaded {} environment properties from {}", count, file.getAbsolutePath());
                } catch (Exception ex) {
                    log.warn("Failed reading .env file: {}", ex.getMessage());
                }
            }
        }
    }

    private static void configureNetworkAndProxy() {
        String preferIpv4 = getSetting("PREFER_IPV4_STACK");
        if ("true".equalsIgnoreCase(preferIpv4)) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        String proxyEnabled = getSetting("PROXY_ENABLED");
        if (!"true".equalsIgnoreCase(proxyEnabled)) {
            return;
        }

        String host = getSetting("PROXY_HOST");
        String port = getSetting("PROXY_PORT");
        if (isBlank(host) || isBlank(port)) {
            throw new IllegalStateException("PROXY_ENABLED=true requires non-blank PROXY_HOST and PROXY_PORT.");
        }

        String nonProxyHosts = getSetting("PROXY_NON_PROXY_HOSTS");
        if (isBlank(nonProxyHosts)) {
            nonProxyHosts = "localhost|127.0.0.1";
        }

        setIfUnset("http.proxyHost", host.trim());
        setIfUnset("http.proxyPort", port.trim());
        setIfUnset("https.proxyHost", host.trim());
        setIfUnset("https.proxyPort", port.trim());
        setIfUnset("http.nonProxyHosts", nonProxyHosts.trim());
        setIfUnset("https.nonProxyHosts", nonProxyHosts.trim());
        log.info("Outbound HTTP/HTTPS proxy enabled via {}:{}", host.trim(), port.trim());
    }

    private static void resolveDatabaseTarget() {
        // Explicit SPRING_DATASOURCE_URL (e.g. Render host env vars) takes precedence with zero fallback.
        String explicitUrl = getSetting("SPRING_DATASOURCE_URL");
        if (!isBlank(explicitUrl)) {
            if (isBlank(getSetting("SPRING_DATASOURCE_USERNAME")) || isBlank(getSetting("SPRING_DATASOURCE_PASSWORD"))) {
                throw new IllegalStateException(
                        "SPRING_DATASOURCE_URL is set, so SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD are also required.");
            }
            return;
        }

        String target = getSetting("DB_TARGET");
        if (isBlank(target)) {
            throw new IllegalStateException(
                    "Database target is not configured. Set DB_TARGET=supabase or DB_TARGET=local (or set SPRING_DATASOURCE_URL directly).");
        }

        String normalized = target.trim().toLowerCase();
        switch (normalized) {
            case "supabase" -> applyDatasource(
                    "SUPABASE",
                    getSetting("SUPABASE_DB_URL"),
                    getSetting("SUPABASE_DB_USERNAME"),
                    getSetting("SUPABASE_DB_PASSWORD")
            );
            case "local" -> applyDatasource(
                    "LOCAL",
                    getSetting("LOCAL_DB_URL"),
                    getSetting("LOCAL_DB_USERNAME"),
                    getSetting("LOCAL_DB_PASSWORD")
            );
            default -> throw new IllegalStateException(
                    "Invalid DB_TARGET='" + target + "'. Allowed values are 'supabase' or 'local' (no fallback).");
        }
    }

    private static void applyDatasource(String label, String url, String username, String password) {
        if (isBlank(url) || isBlank(username) || isBlank(password)) {
            throw new IllegalStateException(
                    "DB_TARGET=" + label.toLowerCase() + " requires " + label + "_DB_URL, " + label
                            + "_DB_USERNAME, and " + label + "_DB_PASSWORD. No fallback database will be used.");
        }
        System.setProperty("SPRING_DATASOURCE_URL", url.trim());
        System.setProperty("SPRING_DATASOURCE_USERNAME", username.trim());
        System.setProperty("SPRING_DATASOURCE_PASSWORD", password.trim());
        log.info("Configured strict database target: {}", label);
    }

    private static String getSetting(String key) {
        String sys = System.getProperty(key);
        return sys != null ? sys : System.getenv(key);
    }

    private static void setIfUnset(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

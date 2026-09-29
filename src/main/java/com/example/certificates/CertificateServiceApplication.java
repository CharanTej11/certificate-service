package com.example.certificates;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
@EnableJpaAuditing
public class CertificateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceApplication.class);
    private static EmbeddedPostgres embeddedPostgres;

    public static void main(String[] args) {
        configureCloudDatabaseUrl();
        ensurePostgresRunning();
        SpringApplication.run(CertificateServiceApplication.class, args);
    }

    private static void configureCloudDatabaseUrl() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.isBlank() && System.getenv("SPRING_DATASOURCE_URL") == null) {
            try {
                // If DATABASE_URL is postgres://user:pass@host:port/db or postgresql://...
                URI uri = new URI(dbUrl.startsWith("postgres://") ? dbUrl.replace("postgres://", "postgresql://") : dbUrl);
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                System.setProperty("spring.datasource.url", jdbcUrl);

                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    System.setProperty("spring.datasource.username", parts[0]);
                    System.setProperty("spring.datasource.password", parts[1]);
                }
                log.info("Auto-configured Spring DataSource from cloud DATABASE_URL for host: {}", host);
            } catch (Exception e) {
                log.warn("Could not parse DATABASE_URL: {}", e.getMessage());
            }
        }
    }

    private static void ensurePostgresRunning() {
        String dsUrl = System.getProperty("spring.datasource.url", System.getenv("SPRING_DATASOURCE_URL"));
        if (dsUrl != null && !dsUrl.contains("localhost") && !dsUrl.contains("127.0.0.1")) {
            log.info("External/Cloud DataSource detected; skipping local embedded PostgreSQL.");
            return;
        }

        if (System.getenv("DATABASE_URL") != null && !System.getenv("DATABASE_URL").contains("localhost")) {
            log.info("Cloud DATABASE_URL detected; skipping local embedded PostgreSQL.");
            return;
        }

        if (isPortListening("localhost", 5432)) {
            log.info("PostgreSQL is already running on port 5432.");
            return;
        }

        log.info("No PostgreSQL detected on port 5432. Starting embedded PostgreSQL 16...");
        try {
            embeddedPostgres = EmbeddedPostgres.builder()
                    .setPort(5432)
                    .start();

            try (Connection conn = embeddedPostgres.getPostgresDatabase().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DO $$ BEGIN " +
                        "IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'certificates') THEN " +
                        "  CREATE ROLE certificates WITH LOGIN SUPERUSER PASSWORD 'certificates'; " +
                        "END IF; " +
                        "END $$;");
                try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = 'certificates'")) {
                    if (!rs.next()) {
                        stmt.execute("CREATE DATABASE certificates OWNER certificates;");
                    }
                }
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (embeddedPostgres != null) {
                    try {
                        log.info("Stopping embedded PostgreSQL...");
                        embeddedPostgres.close();
                    } catch (IOException e) {
                        log.error("Error closing embedded PostgreSQL", e);
                    }
                }
            }));

            log.info("Embedded PostgreSQL started successfully on port 5432.");
        } catch (Exception e) {
            log.error("Failed to start embedded PostgreSQL: {}", e.getMessage(), e);
        }
    }

    private static boolean isPortListening(String host, int port) {
        try (Socket s = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

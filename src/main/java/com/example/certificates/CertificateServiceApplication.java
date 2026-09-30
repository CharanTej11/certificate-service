package com.example.certificates;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
@EnableJpaAuditing
public class CertificateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceApplication.class);
    private static EmbeddedPostgres embeddedPostgres;

    public static void main(String[] args) {
        ensurePostgresRunning();
        SpringApplication.run(CertificateServiceApplication.class, args);
    }

    private static void ensurePostgresRunning() {
        String dsUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (dsUrl != null && !dsUrl.contains("localhost") && !dsUrl.contains("127.0.0.1")) {
            log.info("External/Cloud DataSource detected; skipping local embedded PostgreSQL.");
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

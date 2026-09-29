package com.example.certificates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.Closeable;
import java.net.Socket;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootApplication
@EnableJpaAuditing
public class CertificateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceApplication.class);
    private static Object embeddedPostgres;

    public static void main(String[] args) {
        configureCloudDatabaseUrl();
        ensureLocalPostgresIfAvailable();
        SpringApplication.run(CertificateServiceApplication.class, args);
    }

    private static void configureCloudDatabaseUrl() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && !dbUrl.isBlank() && System.getenv("SPRING_DATASOURCE_URL") == null) {
            try {
                String normalizedUrl = dbUrl.startsWith("postgres://") 
                        ? dbUrl.replace("postgres://", "postgresql://") 
                        : dbUrl;
                URI uri = new URI(normalizedUrl);
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
                log.info("Configured Spring DataSource from cloud DATABASE_URL for host: {}", host);
            } catch (Exception e) {
                log.warn("Could not parse DATABASE_URL: {}", e.getMessage());
            }
        }
    }

    private static void ensureLocalPostgresIfAvailable() {
        String dsUrl = System.getProperty("spring.datasource.url", System.getenv("SPRING_DATASOURCE_URL"));
        if (dsUrl != null && !dsUrl.contains("localhost") && !dsUrl.contains("127.0.0.1")) {
            return;
        }

        if (System.getenv("DATABASE_URL") != null && !System.getenv("DATABASE_URL").contains("localhost")) {
            return;
        }

        if (isPortListening("localhost", 5432)) {
            return;
        }

        try {
            Class<?> builderClass = Class.forName("io.zonky.test.db.postgres.embedded.EmbeddedPostgres");
            Object builder = builderClass.getMethod("builder").invoke(null);
            builder = builder.getClass().getMethod("setPort", int.class).invoke(builder, 5432);
            embeddedPostgres = builder.getClass().getMethod("start").invoke(builder);

            javax.sql.DataSource ds = (javax.sql.DataSource) embeddedPostgres.getClass().getMethod("getPostgresDatabase").invoke(embeddedPostgres);
            try (Connection conn = ds.getConnection();
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
                if (embeddedPostgres instanceof Closeable closeable) {
                    try {
                        closeable.close();
                    } catch (Exception ignored) {}
                }
            }));
            log.info("Embedded PostgreSQL started on port 5432.");
        } catch (ClassNotFoundException e) {
            log.debug("Embedded Postgres not present on classpath; skipping.");
        } catch (Exception e) {
            log.warn("Failed to initialize embedded Postgres: {}", e.getMessage());
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

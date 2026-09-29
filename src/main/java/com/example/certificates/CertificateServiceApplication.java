package com.example.certificates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.Closeable;
import java.net.Socket;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableJpaAuditing
public class CertificateServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceApplication.class);
    private static Object embeddedPostgres;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CertificateServiceApplication.class);
        app.addInitializers(new ApplicationContextInitializer<ConfigurableApplicationContext>() {
            @Override
            public void initialize(ConfigurableApplicationContext applicationContext) {
                normalizeDataSourceProperties(applicationContext.getEnvironment());
            }
        });

        ensureLocalPostgresIfAvailable();
        app.run(args);
    }

    private static void normalizeDataSourceProperties(ConfigurableEnvironment env) {
        String rawUrl = env.getProperty("SPRING_DATASOURCE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = env.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = env.getProperty("spring.datasource.url");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        String trimmed = rawUrl.trim();
        Map<String, Object> map = new HashMap<>();

        if (trimmed.startsWith("jdbc:")) {
            if (trimmed.startsWith("jdbc:postgres://")) {
                String fixed = "jdbc:postgresql://" + trimmed.substring("jdbc:postgres://".length());
                map.put("spring.datasource.url", fixed);
                map.put("SPRING_DATASOURCE_URL", fixed);
                env.getPropertySources().addFirst(new MapPropertySource("cloudDataSourceNormalizer", map));
                log.info("Normalized JDBC DataSource URL: {}", fixed);
            }
            return;
        }

        try {
            String uriString = trimmed.startsWith("postgres://")
                    ? trimmed.replaceFirst("^postgres://", "postgresql://")
                    : trimmed;
            URI uri = new URI(uriString);

            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts[1];
            }

            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath() != null && !uri.getPath().isBlank() ? uri.getPath() : "/certificates";
            String query = uri.getQuery();

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null ? "?" + query : "");

            map.put("spring.datasource.url", jdbcUrl);
            map.put("SPRING_DATASOURCE_URL", jdbcUrl);
            System.setProperty("spring.datasource.url", jdbcUrl);
            System.setProperty("SPRING_DATASOURCE_URL", jdbcUrl);

            if (username != null) {
                map.put("spring.datasource.username", username);
                map.put("SPRING_DATASOURCE_USERNAME", username);
                System.setProperty("spring.datasource.username", username);
            }
            if (password != null) {
                map.put("spring.datasource.password", password);
                map.put("SPRING_DATASOURCE_PASSWORD", password);
                System.setProperty("spring.datasource.password", password);
            }

            env.getPropertySources().addFirst(new MapPropertySource("cloudDataSourceNormalizer", map));
            log.info("Auto-normalized cloud DataSource URL to: {}", jdbcUrl);
        } catch (Exception e) {
            String fallback = "jdbc:" + (trimmed.startsWith("postgres://") ? trimmed.replaceFirst("^postgres://", "postgresql://") : trimmed);
            map.put("spring.datasource.url", fallback);
            map.put("SPRING_DATASOURCE_URL", fallback);
            env.getPropertySources().addFirst(new MapPropertySource("cloudDataSourceNormalizer", map));
            log.warn("Fallback normalized cloud DataSource URL to: {}", fallback);
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

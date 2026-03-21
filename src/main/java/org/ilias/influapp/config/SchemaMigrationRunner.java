package org.ilias.influapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Schema migration runner that creates missing columns in the database.
 * Runs BEFORE TestDataLoader (@Order(0))
 */
@Component
@Order(0)
public class SchemaMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final DataSource dataSource;

    public SchemaMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info("SchemaMigrationRunner starting: checking and creating missing columns...");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Add missing columns to social_media table
            String[] sqlStatements = {
                "ALTER TABLE IF EXISTS social_media ADD COLUMN IF NOT EXISTS average_comments INTEGER DEFAULT 0",
                "ALTER TABLE IF EXISTS social_media ADD COLUMN IF NOT EXISTS average_likes INTEGER DEFAULT 0",
                "ALTER TABLE IF EXISTS social_media ADD COLUMN IF NOT EXISTS engagement_rate DOUBLE PRECISION DEFAULT 0.0",
                "ALTER TABLE IF EXISTS social_media ADD COLUMN IF NOT EXISTS profile_views INTEGER DEFAULT 0"
            };

            for (String sql : sqlStatements) {
                try {
                    stmt.execute(sql);
                    log.info("✓ Executed: {}", sql);
                } catch (SQLException ex) {
                    log.warn("Column may already exist or error: {}", ex.getMessage());
                }
            }

            log.info("✓ SchemaMigrationRunner finished: all columns ready");

        } catch (SQLException ex) {
            log.error("✗ Failed to create columns: {}", ex.getMessage());
            ex.printStackTrace();
        }
    }
}


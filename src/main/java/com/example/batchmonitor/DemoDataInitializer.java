package com.example.batchmonitor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

/** Seeds an empty local H2 database so the dashboard is useful on first launch. */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public DemoDataInitializer(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getURL().startsWith("jdbc:h2:")) return;
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM BATCH_JOB_INSTANCE", Integer.class) > 0) return;

        Instant now = Instant.now();
        jobInstance(1, "salesImport", "sales-import-demo");
        jobInstance(2, "customerExport", "customer-export-demo");
        jobInstance(3, "inventorySync", "inventory-sync-demo");
        jobInstance(4, "reconciliation", "reconciliation-demo");

        execution(1, 1, now.minusSeconds(86_400), now.minusSeconds(86_390), "COMPLETED", "COMPLETED", "100 records imported");
        execution(2, 2, now.minusSeconds(43_200), now.minusSeconds(43_170), "FAILED", "FAILED", "Connection to reporting database timed out");
        execution(3, 3, now.minusSeconds(900), null, "STARTED", "EXECUTING", "Inventory synchronization is still running");
        execution(4, 4, now.minusSeconds(7_200), now.minusSeconds(6_900), "STOPPED", "STOPPED", "Stopped by operator after upstream warning");

        step(1, 1, "readSalesFile", now.minusSeconds(86_400), now.minusSeconds(86_395), "COMPLETED", "COMPLETED", 100, 100, 1, 0, "");
        step(2, 1, "writeSalesRecords", now.minusSeconds(86_395), now.minusSeconds(86_390), "COMPLETED", "COMPLETED", 100, 100, 1, 0, "");
        step(3, 2, "extractCustomers", now.minusSeconds(43_200), now.minusSeconds(43_190), "COMPLETED", "COMPLETED", 250, 250, 3, 0, "");
        step(4, 2, "publishCustomerFile", now.minusSeconds(43_190), now.minusSeconds(43_170), "FAILED", "FAILED", 250, 0, 0, 1, "Reporting database connection timed out");
        step(5, 3, "syncInventory", now.minusSeconds(900), null, "STARTED", "EXECUTING", 1_240, 1_180, 12, 0, "");
        step(6, 4, "validateBalances", now.minusSeconds(7_200), now.minusSeconds(7_100), "COMPLETED", "COMPLETED", 50, 50, 1, 0, "");
        step(7, 4, "applyAdjustments", now.minusSeconds(7_100), now.minusSeconds(6_900), "STOPPED", "STOPPED", 50, 35, 0, 0, "Stopped before committing remaining adjustments");

        parameter(1, "businessDate", "STRING", "2026-10-06", null);
        parameter(2, "businessDate", "STRING", "2026-10-06", null);
        parameter(3, "businessDate", "STRING", "2026-10-07", null);
        parameter(4, "businessDate", "STRING", "2026-10-07", null);
        context(1, "{\"recordsImported\":100,\"source\":\"sales.csv\"}");
        context(2, "{\"lastSuccessfulStep\":\"extractCustomers\"}");
        context(3, "{\"processedSku\":1240,\"remainingSku\":360}");
        context(4, "{\"validatedAccounts\":50,\"adjustmentsApplied\":35}");
    }

    private void jobInstance(long id, String name, String key) {
        jdbc.update("INSERT INTO BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, VERSION, JOB_NAME, JOB_KEY) VALUES (?, 0, ?, ?)", id, name, key);
    }

    private void execution(long id, long instanceId, Instant start, Instant end, String status, String exitCode, String message) {
        jdbc.update("""
            INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED)
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?)
            """, id, instanceId, timestamp(start), timestamp(start), timestamp(end), status, exitCode, message, timestamp(end == null ? start : end));
    }

    private void step(long id, long executionId, String name, Instant start, Instant end, String status, String exitCode,
                      long read, long write, long commits, long rollbacks, String message) {
        jdbc.update("""
            INSERT INTO BATCH_STEP_EXECUTION (STEP_EXECUTION_ID, VERSION, STEP_NAME, JOB_EXECUTION_ID, START_TIME, END_TIME, STATUS,
              COMMIT_COUNT, READ_COUNT, FILTER_COUNT, WRITE_COUNT, EXIT_CODE, EXIT_MESSAGE, READ_SKIP_COUNT, WRITE_SKIP_COUNT,
              PROCESS_SKIP_COUNT, ROLLBACK_COUNT, LAST_UPDATED)
            VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, 0, 0, 0, ?, ?)
            """, id, name, executionId, timestamp(start), timestamp(end), status, commits, read, write, exitCode, message, rollbacks, timestamp(end == null ? start : end));
    }

    private void parameter(long executionId, String name, String type, String value, Long ignored) {
        jdbc.update("""
            INSERT INTO BATCH_JOB_EXECUTION_PARAMS (JOB_EXECUTION_ID, TYPE_CD, KEY_NAME, STRING_VAL, DATE_VAL, LONG_VAL, DOUBLE_VAL, IDENTIFYING)
            VALUES (?, ?, ?, ?, NULL, NULL, NULL, 'Y')
            """, executionId, type, name, value);
    }

    private void context(long executionId, String value) {
        jdbc.update("INSERT INTO BATCH_JOB_EXECUTION_CONTEXT (JOB_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT) VALUES (?, ?, NULL)", executionId, value);
    }

    private Timestamp timestamp(Instant instant) { return instant == null ? null : Timestamp.from(instant); }
}

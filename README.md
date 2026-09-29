# Spring Batch Monitor

A small, read-only dashboard for the standard Spring Batch metadata tables. It recreates the useful part of the old Spring Batch Admin experience: recent executions, status filtering, execution drill-down, step metrics, and exit/failure messages.

## Run

```text
mvn spring-boot:run
```

Open http://localhost:8080.

By default the app uses an in-memory H2 database and creates the three metadata tables needed by the dashboard. To point it at an existing application database, set `BATCH_MONITOR_DATASOURCE_URL`, `BATCH_MONITOR_DATASOURCE_USERNAME`, `BATCH_MONITOR_DATASOURCE_PASSWORD`, and `BATCH_MONITOR_DATASOURCE_DRIVER`. Set `BATCH_MONITOR_SQL_INIT_MODE=never` when the target already owns its schema.

The metadata table prefix is currently `BATCH_`, matching Spring Batch's default. The next production-hardening step is to make the prefix configurable in the repository SQL and add authentication before exposing this outside an internal network.

The design follows Spring Batch Admin's historical workflow: inspect the latest executions, open one execution, then drill into its steps and failure detail. It intentionally does not launch, stop, or abandon jobs yet.

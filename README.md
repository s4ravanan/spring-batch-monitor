# Spring Batch Monitor

A small, read-only dashboard for the standard Spring Batch metadata tables. It recreates the useful part of the old Spring Batch Admin experience: recent executions, status filtering, execution drill-down, step metrics, and exit/failure messages.

## Run

```text
mvn spring-boot:run
```

Open http://localhost:8080.

By default the app uses an in-memory H2 database and creates the three metadata tables needed by the dashboard. To point it at an existing application database, set `BATCH_MONITOR_DATASOURCE_URL`, `BATCH_MONITOR_DATASOURCE_USERNAME`, `BATCH_MONITOR_DATASOURCE_PASSWORD`, and `BATCH_MONITOR_DATASOURCE_DRIVER`. Set `BATCH_MONITOR_SQL_INIT_MODE=never` when the target already owns its schema.

The metadata table prefix is currently `BATCH_`, matching Spring Batch's default. The next production-hardening step is to make the prefix configurable in the repository SQL and add authentication before exposing this outside an internal network.

The design follows Spring Batch Admin's historical workflow: inspect the latest executions, open one execution, then drill into its steps and failure detail.

The monitor now also exposes the legacy operational API when it is embedded in an application that provides Spring Batch's `JobRegistry`, `JobOperator`, and `JobExplorer` beans:

- `GET /api/jobs` lists discovered and launchable jobs.
- `GET /api/jobs/{jobName}/executions` lists executions and instances for one job.
- `POST /api/jobs/{jobName}/launch` launches a job with a JSON object of parameters.
- `POST /api/jobs/{jobName}/next` starts the next incremented instance.
- `POST /api/executions/{id}/stop` sends a stop signal.
- `POST /api/executions/{id}/abandon` marks a stopped execution abandoned.
- `POST /api/executions/{id}/restart` restarts a failed execution.

These operations are deliberately unavailable in the standalone viewer until the host application supplies the corresponding Spring Batch beans.

The Files API mirrors the old admin console's file area while keeping all paths below one configured directory:

- `GET /api/files?path=` lists files.
- `POST /api/files` accepts multipart field `file` and an optional `path`.
- `GET /api/files/content?path=` downloads a file.
- `DELETE /api/files?path=` deletes a file.

For an internal development instance, authentication is disabled by default. Enable HTTP Basic authentication for a deployed monitor with `BATCH_MONITOR_SECURITY_ENABLED=true`, `BATCH_MONITOR_SECURITY_USERNAME`, and a strong `BATCH_MONITOR_SECURITY_PASSWORD`. Change the default password whenever authentication is enabled.

Configure the root with `BATCH_MONITOR_UPLOAD_DIR`. Configuration files are stored only; they are not dynamically parsed or loaded as application code.

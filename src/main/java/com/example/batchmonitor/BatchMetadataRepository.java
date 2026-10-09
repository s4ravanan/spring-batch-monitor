package com.example.batchmonitor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class BatchMetadataRepository {
    private final JdbcTemplate jdbc;

    public BatchMetadataRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> latestExecutions(int limit, String status) {
        var sql = """
            SELECT ji.JOB_NAME AS "jobName", je.JOB_EXECUTION_ID AS "executionId",
                   je.STATUS AS "status", je.EXIT_CODE AS "exitCode",
                   je.START_TIME AS "startTime", je.END_TIME AS "endTime",
                   je.CREATE_TIME AS "createTime", je.EXIT_MESSAGE AS "exitMessage"
            FROM BATCH_JOB_EXECUTION je
            JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
            WHERE (? IS NULL OR je.STATUS = ?)
            ORDER BY COALESCE(je.START_TIME, je.CREATE_TIME) DESC
            LIMIT ?
            """;
        return jdbc.queryForList(sql, status, status, limit);
    }

    public List<String> jobNames() {
        return jdbc.queryForList("SELECT DISTINCT JOB_NAME FROM BATCH_JOB_INSTANCE ORDER BY JOB_NAME", String.class);
    }

    public List<Map<String, Object>> executionsForJob(String jobName, int limit) {
        return jdbc.queryForList("""
            SELECT ji.JOB_NAME AS "jobName", ji.JOB_INSTANCE_ID AS "instanceId",
                   je.JOB_EXECUTION_ID AS "executionId", je.STATUS AS "status",
                   je.EXIT_CODE AS "exitCode", je.START_TIME AS "startTime", je.END_TIME AS "endTime",
                   je.CREATE_TIME AS "createTime", je.EXIT_MESSAGE AS "exitMessage"
            FROM BATCH_JOB_EXECUTION je
            JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
            WHERE ji.JOB_NAME = ?
            ORDER BY COALESCE(je.START_TIME, je.CREATE_TIME) DESC LIMIT ?
            """, jobName, Math.max(1, Math.min(limit, 200)));
    }

    public List<Map<String, Object>> parameters(long executionId) {
        return jdbc.queryForList("""
            SELECT KEY_NAME AS "name", TYPE_CD AS "type", STRING_VAL AS "stringValue",
                   DATE_VAL AS "dateValue", LONG_VAL AS "longValue", DOUBLE_VAL AS "doubleValue",
                   IDENTIFYING AS "identifying"
            FROM BATCH_JOB_EXECUTION_PARAMS WHERE JOB_EXECUTION_ID = ? ORDER BY KEY_NAME
            """, executionId);
    }

    public Map<String, Object> executionContext(long executionId) {
        return jdbc.queryForMap("""
            SELECT SHORT_CONTEXT AS "shortContext", SERIALIZED_CONTEXT AS "serializedContext"
            FROM BATCH_JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ?
            """, executionId);
    }

    public List<Map<String, Object>> stepContexts(long executionId) {
        return jdbc.queryForList("""
            SELECT se.STEP_EXECUTION_ID AS "stepExecutionId", sec.SHORT_CONTEXT AS "shortContext",
                   sec.SERIALIZED_CONTEXT AS "serializedContext"
            FROM BATCH_STEP_EXECUTION_CONTEXT sec
            JOIN BATCH_STEP_EXECUTION se ON se.STEP_EXECUTION_ID = sec.STEP_EXECUTION_ID
            WHERE se.JOB_EXECUTION_ID = ? ORDER BY se.STEP_EXECUTION_ID
            """, executionId);
    }

    public List<Map<String, Object>> instances(String jobName, int limit) {
        return jdbc.queryForList("""
            SELECT JOB_INSTANCE_ID AS "instanceId", JOB_NAME AS "jobName", JOB_KEY AS "jobKey"
            FROM BATCH_JOB_INSTANCE WHERE JOB_NAME = ? ORDER BY JOB_INSTANCE_ID DESC LIMIT ?
            """, jobName, Math.max(1, Math.min(limit, 200)));
    }

    public Map<String, Object> execution(long id) {
        return jdbc.queryForMap("""
            SELECT ji.JOB_NAME AS "jobName", ji.JOB_KEY AS "jobKey",
                   je.JOB_EXECUTION_ID AS "executionId", je.STATUS AS "status",
                   je.EXIT_CODE AS "exitCode", je.EXIT_MESSAGE AS "exitMessage",
                   je.START_TIME AS "startTime", je.END_TIME AS "endTime",
                   je.CREATE_TIME AS "createTime", je.LAST_UPDATED AS "lastUpdated"
            FROM BATCH_JOB_EXECUTION je
            JOIN BATCH_JOB_INSTANCE ji ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
            WHERE je.JOB_EXECUTION_ID = ?
            """, id);
    }

    public List<Map<String, Object>> steps(long id) {
        return jdbc.queryForList("""
            SELECT STEP_EXECUTION_ID AS "stepExecutionId", STEP_NAME AS "stepName",
                   STATUS AS "status", EXIT_CODE AS "exitCode", EXIT_MESSAGE AS "exitMessage",
                   START_TIME AS "startTime", END_TIME AS "endTime",
                   READ_COUNT AS "readCount", WRITE_COUNT AS "writeCount",
                   COMMIT_COUNT AS "commitCount", READ_SKIP_COUNT AS "readSkipCount",
                   WRITE_SKIP_COUNT AS "writeSkipCount", PROCESS_SKIP_COUNT AS "processSkipCount",
                   FILTER_COUNT AS "filterCount"
            FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID = ?
            ORDER BY STEP_EXECUTION_ID
            """, id);
    }

    public long count(String status) {
        var sql = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE (? IS NULL OR STATUS = ?)";
        return jdbc.queryForObject(sql, Long.class, status, status);
    }
}

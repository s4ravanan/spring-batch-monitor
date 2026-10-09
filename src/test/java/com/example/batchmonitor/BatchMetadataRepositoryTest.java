package com.example.batchmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BatchMetadataRepositoryTest {

    @Autowired
    private BatchMetadataRepository repository;

    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Test
    void latestExecutionsReturnsCamelCaseKeys() {
        List<Map<String, Object>> executions = repository.latestExecutions(10, null);
        assertFalse(executions.isEmpty());
        Map<String, Object> first = executions.get(0);
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(first));
        assertTrue(json.contains("\"jobName\":"), "JSON should contain '\"jobName\":' but was: " + json);
        assertTrue(json.contains("\"executionId\":"), "JSON should contain '\"executionId\":' but was: " + json);
        assertTrue(json.contains("\"status\":"), "JSON should contain '\"status\":' but was: " + json);
        assertTrue(json.contains("\"exitCode\":"), "JSON should contain '\"exitCode\":' but was: " + json);
        assertTrue(json.contains("\"startTime\":"), "JSON should contain '\"startTime\":' but was: " + json);
    }

    @Test
    void executionDetailAndStepsReturnCamelCaseKeys() {
        Map<String, Object> execution = repository.execution(1);
        String jsonExecution = assertDoesNotThrow(() -> mapper.writeValueAsString(execution));
        assertTrue(jsonExecution.contains("\"jobName\":"), "Execution JSON should contain 'jobName'");
        assertTrue(jsonExecution.contains("\"executionId\":"), "Execution JSON should contain 'executionId'");

        List<Map<String, Object>> steps = repository.steps(1);
        assertFalse(steps.isEmpty());
        String jsonStep = assertDoesNotThrow(() -> mapper.writeValueAsString(steps.get(0)));
        assertTrue(jsonStep.contains("\"stepName\":"), "Step JSON should contain 'stepName'");
        assertTrue(jsonStep.contains("\"stepExecutionId\":"), "Step JSON should contain 'stepExecutionId'");
        assertTrue(jsonStep.contains("\"readCount\":"), "Step JSON should contain 'readCount'");

        List<Map<String, Object>> params = repository.parameters(1);
        assertFalse(params.isEmpty());
        String jsonParam = assertDoesNotThrow(() -> mapper.writeValueAsString(params.get(0)));
        assertTrue(jsonParam.contains("\"name\":"), "Param JSON should contain 'name'");
        assertTrue(jsonParam.contains("\"stringValue\":"), "Param JSON should contain 'stringValue'");
    }
}

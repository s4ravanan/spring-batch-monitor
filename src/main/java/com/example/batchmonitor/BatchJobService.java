package com.example.batchmonitor;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

@Service
public class BatchJobService {
    private final BatchMetadataRepository metadata;
    private final ObjectProvider<JobRegistry> registry;
    private final ObjectProvider<JobOperator> operator;
    private final ObjectProvider<JobExplorer> explorer;

    public BatchJobService(BatchMetadataRepository metadata, ObjectProvider<JobRegistry> registry,
                           ObjectProvider<JobOperator> operator, ObjectProvider<JobExplorer> explorer) {
        this.metadata = metadata; this.registry = registry; this.operator = operator; this.explorer = explorer;
    }

    public Set<String> allJobNames() {
        var names = new LinkedHashSet<>(metadata.jobNames());
        if (registry.getIfAvailable() != null) names.addAll(registry.getObject().getJobNames());
        return names;
    }

    public Set<String> launchableJobNames() {
        var r = registry.getIfAvailable();
        return r == null ? Set.of() : new LinkedHashSet<>(r.getJobNames());
    }

    public long launch(String jobName, Properties parameters) throws Exception {
        return requireOperator().start(jobName, parameters);
    }

    public long next(String jobName) throws Exception { return requireOperator().startNextInstance(jobName); }

    public boolean stop(long executionId) throws Exception { return requireOperator().stop(executionId); }
    public void abandon(long executionId) throws Exception { requireOperator().abandon(executionId); }
    public long restart(long executionId) throws Exception { return requireOperator().restart(executionId); }

    private JobOperator requireOperator() {
        var value = operator.getIfAvailable();
        if (value == null) throw new IllegalStateException("Operational controls require a Spring Batch JobOperator bean");
        return value;
    }

    private JobExplorer requireExplorer() {
        var value = explorer.getIfAvailable();
        if (value == null) throw new IllegalStateException("The Spring Batch JobExplorer bean is not configured");
        return value;
    }
}

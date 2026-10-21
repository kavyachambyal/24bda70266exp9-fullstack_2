package edu.cu.releaselab;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PipelineService {
    private static final int[] CANARY_STEPS = {10, 25, 50, 100};

    private int runNumber = 0;
    private int canaryIndex = -1;
    private String lastRunAt = null;

    public synchronized Map<String, Object> overview() {
        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("status", runNumber == 0 ? "ready" : "passed");
        pipeline.put("runNumber", runNumber);
        pipeline.put("lastRunAt", lastRunAt);

        Map<String, Object> deployment = new LinkedHashMap<>();
        deployment.put("status", canaryIndex < 0 ? "idle" : canaryIndex == CANARY_STEPS.length - 1 ? "live" : "canary");
        deployment.put("trafficPercent", canaryIndex < 0 ? 0 : CANARY_STEPS[canaryIndex]);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "release-lab-api");
        response.put("pipeline", pipeline);
        response.put("deployment", deployment);
        response.put("checks", Map.of(
                "unit", Map.of("label", "Unit tests", "result", "passing", "count", 3),
                "api", Map.of("label", "API integration", "result", "passing", "count", 4),
                "contract", Map.of("label", "Pact contract", "result", "passing", "count", 1),
                "container", Map.of("label", "Container build", "result", "passing", "count", 1)));
        return response;
    }

    public synchronized Map<String, Object> runPipeline() {
        runNumber++;
        lastRunAt = Instant.now().toString();
        return overview();
    }

    public synchronized Map<String, Object> startCanary() {
        if (runNumber == 0) {
            throw new IllegalStateException("Run the pipeline successfully before starting a canary deployment.");
        }
        canaryIndex = 0;
        return overview();
    }

    public synchronized Map<String, Object> promoteCanary() {
        if (canaryIndex < 0) {
            throw new IllegalStateException("Start a canary deployment before promoting it.");
        }
        canaryIndex = Math.min(canaryIndex + 1, CANARY_STEPS.length - 1);
        return overview();
    }

    public synchronized Map<String, Object> rollback() {
        canaryIndex = -1;
        return overview();
    }
}

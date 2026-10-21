package edu.cu.releaselab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {
    @Test
    void pipelineRunAdvancesTheBuildNumber() {
        PipelineService service = new PipelineService();

        Map<String, Object> firstRun = service.runPipeline();
        Map<String, Object> secondRun = service.runPipeline();

        assertEquals(1, firstRun.get("pipeline") instanceof Map<?, ?> pipeline ? pipeline.get("runNumber") : null);
        assertEquals(2, secondRun.get("pipeline") instanceof Map<?, ?> pipeline ? pipeline.get("runNumber") : null);
    }

    @Test
    void canaryCannotStartUntilThePipelineHasPassed() {
        PipelineService service = new PipelineService();

        IllegalStateException error = assertThrows(IllegalStateException.class, service::startCanary);

        assertEquals("Run the pipeline successfully before starting a canary deployment.", error.getMessage());
    }

    @Test
    void canaryProgressesThroughTheTrafficRolloutStages() {
        PipelineService service = new PipelineService();
        service.runPipeline();

        assertEquals(10, trafficPercent(service.startCanary()));
        assertEquals(25, trafficPercent(service.promoteCanary()));
        assertEquals(50, trafficPercent(service.promoteCanary()));
        assertEquals(100, trafficPercent(service.promoteCanary()));
    }

    private int trafficPercent(Map<String, Object> overview) {
        Map<?, ?> deployment = (Map<?, ?>) overview.get("deployment");
        return (int) deployment.get("trafficPercent");
    }
}

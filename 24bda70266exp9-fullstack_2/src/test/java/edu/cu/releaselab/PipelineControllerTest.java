package edu.cu.releaselab;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PipelineControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewExposesTheChecksNeededByTheDashboard() throws Exception {
        mockMvc.perform(get("/api/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service", is("release-lab-api")))
                .andExpect(jsonPath("$.checks.unit.result", is("passing")))
                .andExpect(jsonPath("$.checks.contract.result", is("passing")));
    }

    @Test
    void pipelineRunIncrementsItsRunNumber() throws Exception {
        mockMvc.perform(post("/api/pipeline/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipeline.runNumber").value(1))
                .andExpect(jsonPath("$.pipeline.status", is("passed")));
    }

    @Test
    void canaryRequiresAnInitialDeploymentBeforePromotion() throws Exception {
        mockMvc.perform(post("/api/deployment/promote"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Start a canary deployment before promoting it."));
    }

    @Test
    void canaryCanBePromotedAndRolledBack() throws Exception {
        mockMvc.perform(post("/api/pipeline/run"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/deployment/canary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployment.trafficPercent").value(10));
        mockMvc.perform(post("/api/deployment/promote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployment.trafficPercent").value(25));
        mockMvc.perform(post("/api/deployment/rollback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployment.status", is("idle")));
    }
}

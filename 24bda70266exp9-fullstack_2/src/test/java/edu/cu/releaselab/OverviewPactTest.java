package edu.cu.releaselab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import au.com.dius.pact.core.model.annotations.Pact;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "release-lab-api", providerType = ProviderType.SYNCH)
@PactDirectory("target/pacts")
class OverviewPactTest {
    @Pact(consumer = "release-lab-dashboard")
    V4Pact overviewContract(PactDslWithProvider builder) {
        return builder
                .given("the dashboard can load its overview")
                .uponReceiving("a request for the CI/CD dashboard overview")
                .path("/api/overview")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("service", "release-lab-api")
                        .object("pipeline")
                        .stringType("status", "ready")
                        .closeObject()
                        .object("deployment")
                        .stringType("status", "idle")
                        .integerType("trafficPercent", 0)
                        .closeObject()
                        .object("checks")
                        .object("unit").stringType("result", "passing").closeObject()
                        .object("contract").stringType("result", "passing").closeObject()
                        .closeObject())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "overviewContract")
    void dashboardContractCanReadOverview(MockServer mockServer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(mockServer.getUrl() + "/api/overview"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"service\":\"release-lab-api\""));
        assertTrue(response.body().contains("\"trafficPercent\":0"));
    }
}

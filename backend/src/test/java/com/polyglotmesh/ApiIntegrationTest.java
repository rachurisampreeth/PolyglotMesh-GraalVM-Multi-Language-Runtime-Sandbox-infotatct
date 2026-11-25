package com.polyglotmesh;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1", "mesh.mock-delay-ms=0"})
@AutoConfigureWebTestClient
class ApiIntegrationTest {
    @Autowired WebTestClient client;
    @Test void healthReturnsUp() { client.get().uri("/api/health").exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP"); }
    @Test void invalidLanguageReturns400() {
        client.post().uri("/api/execute").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"mode\":\"single\",\"language\":\"java\",\"code\":\"System.out.println(1);\"}")
            .exchange().expectStatus().isBadRequest();
    }
    @Test void dataCollectionsAvailable() { client.get().uri("/api/data/products").exchange().expectStatus().isOk().expectBody().jsonPath("$[0].id").exists(); }
    @Test void mockPriceComputesFormula() {
        client.post().uri("/mock/price").contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"productId\":\"p2\",\"quantity\":2,\"discountPercent\":10}")
            .exchange().expectStatus().isOk().expectBody().jsonPath("$.total").isEqualTo(2700.0);
    }
}

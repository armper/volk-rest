package com.pereatech.volk.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.pereatech.volk.rest.security.ApiKeyFilter;

/**
 * Integration test — requires a MongoDB instance on localhost:27017
 * (see docker-compose.yml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"volk.security.api-key=test-key",
				"spring.data.mongodb.database=volk-test"
		})
class ApiKeyFilterTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void rejectsRequestsWithoutKey() {
		webTestClient.get().uri("/searchuser/findall").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void acceptsRequestsWithKey() {
		webTestClient.get().uri("/searchuser/findall")
				.header(ApiKeyFilter.API_KEY_HEADER, "test-key")
				.exchange().expectStatus().isOk();
	}

	@Test
	void healthStaysOpen() {
		webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
	}
}

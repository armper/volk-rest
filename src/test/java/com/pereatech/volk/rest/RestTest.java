package com.pereatech.volk.rest;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.pereatech.volk.rest.model.SearchFile;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import reactor.core.publisher.Mono;

/**
 * Integration test — requires a MongoDB instance on localhost:27017
 * (see docker-compose.yml).
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestTest {

	@Autowired
	private WebTestClient webTestClient;

	private SearchFile searchFile;

	private final Faker faker = new Faker();

	@BeforeEach
	void setUp() {
		searchFile = new SearchFile();
		searchFile.setFileName(faker.file().fileName());
		searchFile.setExtension(faker.file().extension());
		searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(),
				searchFile.getExtension(), "\\"));
		searchFile.setCreatedDateTime(LocalDateTime.now());
		searchFile.setServer(faker.gameOfThrones().dragon());
		searchFile.setLastModified(LocalDateTime.now());
		log.debug("test file {}", searchFile);
	}

	@Test
	void saveReturnsCreatedFile() {
		webTestClient.post().uri("/searchfile").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).body(Mono.just(searchFile), SearchFile.class).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.fileName").isEqualTo(searchFile.getFileName());
	}

	@Test
	void findAllReturnsFiles() {
		webTestClient.get().uri("/searchfile/findall")
				.accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk()
				.expectBodyList(SearchFile.class).returnResult()
				.getResponseBody().forEach(file -> log.debug("{}", file));
	}
}

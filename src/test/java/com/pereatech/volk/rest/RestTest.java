package com.pereatech.volk.rest;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import reactor.core.publisher.Mono;

/**
 * Integration test — requires a MongoDB instance on localhost:27017
 * (see docker-compose.yml).
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.data.mongodb.database=volk-test")
class RestTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private SearchFileRepository searchFileRepository;

	@Autowired
	private SearchUserRepository searchUserRepository;

	private SearchFile searchFile;

	private final Faker faker = new Faker();

	@BeforeEach
	void setUp() {
		clearTestData();

		searchFile = new SearchFile();
		searchFile.setFileName(faker.file().fileName());
		searchFile.setExtension(faker.file().extension());
		searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(),
				searchFile.getExtension(), "\\"));
		searchFile.setCreatedDateTime(LocalDateTime.now());
		searchFile.setServer(faker.gameOfThrones().dragon());
		searchFile.setLastModified(LocalDateTime.now());
		searchFile.setAccessControlSource("TEST");
		searchFile.setIndexerUser(System.getProperty("user.name"));
		searchFile.setIndexerReadable(true);
		searchFile.setFileOwner(System.getProperty("user.name"));
		searchFile.setOwnerReadable(true);
		searchFile.setSourceId("source-test");
		searchFile.setSourceName("Policy library");
		searchFile.setSourceType("SHARED_DRIVE");
		searchFile.setSourceRoot("/sources/policies");
		searchFile.setRelativePath("annual/" + searchFile.getFileName());
		searchFile.setContentOwner("Records team");
		searchFile.setOwnershipBasis("SOURCE_PROFILE");
		searchFile.setDepartment("Compliance");
	}

	@AfterEach
	void tearDown() {
		clearTestData();
	}

	private void clearTestData() {
		searchFileRepository.deleteAll().block();
		searchUserRepository.deleteAll().block();
	}

	@Test
	void saveReturnsCreatedFile() {
		webTestClient.post().uri("/searchfile").contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).body(Mono.just(searchFile), SearchFile.class).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.fileName").isEqualTo(searchFile.getFileName())
				.jsonPath("$.sourceName").isEqualTo("Policy library")
				.jsonPath("$.contentOwner").isEqualTo("Records team")
				.jsonPath("$.department").isEqualTo("Compliance")
				.jsonPath("$.id").isNotEmpty();
	}

	@Test
	void savingTheSamePathTwiceDoesNotDuplicate() {
		String firstId = webTestClient.post().uri("/searchfile").contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(searchFile), SearchFile.class).exchange()
				.expectStatus().isOk().returnResult(SearchFile.class)
				.getResponseBody().blockFirst().getId();

		webTestClient.post().uri("/searchfile").contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(searchFile), SearchFile.class).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.id").isEqualTo(firstId);
	}

	@Test
	void postUserIsGetOrCreate() {
		SearchUser user = new SearchUser();
		user.setName(faker.name().fullName());
		user.setDomainName(faker.ancient().god());

		String firstId = webTestClient.post().uri("/searchuser").contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(user), SearchUser.class).exchange()
				.expectStatus().isOk().returnResult(SearchUser.class)
				.getResponseBody().blockFirst().getId();

		webTestClient.post().uri("/searchuser").contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(user), SearchUser.class).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.id").isEqualTo(firstId);
	}

	@Test
	void findAllReturnsFiles() {
		searchFileRepository.save(searchFile).block();

		webTestClient.get().uri("/searchfile/findall")
				.accept(MediaType.APPLICATION_JSON).exchange()
				.expectStatus().isOk()
				.expectBodyList(SearchFile.class).hasSize(1);
	}

	@Test
	void inaccessibleFilesAreHiddenFromEveryReadPath() {
		searchFile.setFileName("restricted-volk-test.txt");
		searchFile.setPath("/restricted/restricted-volk-test.txt");
		searchFile.setIndexerUser("another-user");
		searchFile.setIndexerReadable(false);
		searchFile.setFileOwner("another-user");
		searchFile.setOwnerReadable(true);
		searchFile.setGroupReadable(false);
		searchFile.setOthersReadable(false);
		String id = searchFileRepository.save(searchFile).block().getId();

		webTestClient.get().uri("/searchfile/{id}", id).exchange()
				.expectStatus().isNotFound();

		webTestClient.get().uri("/searchfile/findall").exchange()
				.expectStatus().isOk()
				.expectBodyList(SearchFile.class).hasSize(0);
	}
}

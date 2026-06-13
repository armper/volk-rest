package com.pereatech.volk.rest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

	@TempDir
	Path tempDirectory;

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

		webTestClient.get().uri("/searchfile/{id}/preview", id).exchange()
				.expectStatus().isNotFound();

		webTestClient.get().uri("/searchfile/{id}/content", id).exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void previewReturnsExtractedTextForOfficeDocuments() {
		searchFile.setExtension("docx");
		searchFile.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		searchFile.setTitle("Records handbook");
		searchFile.setContent("First line\nSecond line");
		String id = searchFileRepository.save(searchFile).block().getId();

		webTestClient.get().uri("/searchfile/{id}/preview", id).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.previewType").isEqualTo("TEXT")
				.jsonPath("$.originalAvailable").isEqualTo(false)
				.jsonPath("$.text").isEqualTo("First line\nSecond line")
				.jsonPath("$.title").isEqualTo("Records handbook");
	}

	@Test
	void previewStreamsAllowlistedFilesInsideTheirIndexedSource() throws Exception {
		Path source = Files.createDirectory(tempDirectory.resolve("source"));
		Path pdf = source.resolve("sample.pdf");
		byte[] content = "%PDF-1.4 preview test".getBytes(StandardCharsets.UTF_8);
		Files.write(pdf, content);

		searchFile.setFileName(pdf.getFileName().toString());
		searchFile.setPath(pdf.toString());
		searchFile.setSourceRoot(source.toString());
		searchFile.setExtension("pdf");
		searchFile.setContentType(MediaType.APPLICATION_PDF_VALUE);
		searchFile.setContent("Preview test text");
		String id = searchFileRepository.save(searchFile).block().getId();

		webTestClient.get().uri("/searchfile/{id}/preview", id).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.previewType").isEqualTo("PDF")
				.jsonPath("$.originalAvailable").isEqualTo(true);

		webTestClient.get().uri("/searchfile/{id}/content", id).exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_PDF)
				.expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
				.expectBody().consumeWith(result -> org.assertj.core.api.Assertions
						.assertThat(result.getResponseBody()).isEqualTo(content));
	}

	@Test
	void previewDoesNotStreamFilesOutsideTheirIndexedSource() throws Exception {
		Path source = Files.createDirectory(tempDirectory.resolve("source"));
		Path outside = tempDirectory.resolve("outside.pdf");
		Files.writeString(outside, "%PDF-1.4 outside");

		searchFile.setFileName(outside.getFileName().toString());
		searchFile.setPath(outside.toString());
		searchFile.setSourceRoot(source.toString());
		searchFile.setExtension("pdf");
		searchFile.setContentType(MediaType.APPLICATION_PDF_VALUE);
		String id = searchFileRepository.save(searchFile).block().getId();

		webTestClient.get().uri("/searchfile/{id}/preview", id).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.previewType").isEqualTo("UNAVAILABLE")
				.jsonPath("$.originalAvailable").isEqualTo(false);

		webTestClient.get().uri("/searchfile/{id}/content", id).exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void searchFiltersAndSortsReadableDocuments() {
		SearchFile olderPdf = copySearchFile("annual-policy.pdf", "/sources/policies/annual-policy.pdf");
		olderPdf.setExtension("pdf");
		olderPdf.setContentOwner("Records team");
		olderPdf.setAuthor("Alex Writer");
		olderPdf.setKeywords("retention compliance");
		olderPdf.setSize(4_000L);
		olderPdf.setLastModified(LocalDateTime.now().minusDays(5));

		SearchFile newerPdf = copySearchFile("current-policy.pdf", "/sources/policies/current-policy.pdf");
		newerPdf.setExtension("pdf");
		newerPdf.setContentOwner("Records team");
		newerPdf.setAuthor("Alex Writer");
		newerPdf.setKeywords("retention compliance");
		newerPdf.setSize(8_000L);
		newerPdf.setLastModified(LocalDateTime.now());

		SearchFile unrelated = copySearchFile("budget.xlsx", "/sources/finance/budget.xlsx");
		unrelated.setExtension("xlsx");
		unrelated.setContentOwner("Finance team");
		unrelated.setSourceId("finance-source");
		unrelated.setSourceName("Finance records");

		searchFileRepository.saveAll(java.util.List.of(olderPdf, newerPdf, unrelated)).collectList().block();

		webTestClient.get().uri(uriBuilder -> uriBuilder.path("/searchfile/search")
				.queryParam("extension", "pdf")
				.queryParam("owner", "Records team")
				.queryParam("author", "Alex")
				.queryParam("keywords", "retention")
				.queryParam("folder", "policies")
				.queryParam("minSize", 3_000)
				.queryParam("sort", "newest")
				.build()).exchange()
				.expectStatus().isOk()
				.expectBodyList(SearchFile.class)
				.hasSize(2)
				.value(files -> {
					org.assertj.core.api.Assertions.assertThat(files.get(0).getFileName())
							.isEqualTo("current-policy.pdf");
					org.assertj.core.api.Assertions.assertThat(files.get(1).getFileName())
							.isEqualTo("annual-policy.pdf");
				});
	}

	@Test
	void searchOptionsDoNotRevealRestrictedDocuments() {
		SearchFile readable = copySearchFile("readable.pdf", "/sources/readable.pdf");
		readable.setExtension("pdf");
		readable.setContentOwner("Visible owner");

		SearchFile restricted = copySearchFile("restricted.docx", "/restricted/restricted.docx");
		restricted.setExtension("docx");
		restricted.setContentOwner("Secret owner");
		restricted.setIndexerUser("another-user");
		restricted.setIndexerReadable(false);
		restricted.setFileOwner("another-user");
		restricted.setOthersReadable(false);

		searchFileRepository.saveAll(java.util.List.of(readable, restricted)).collectList().block();

		webTestClient.get().uri("/searchfile/search/options").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.extensions.length()").isEqualTo(1)
				.jsonPath("$.extensions[0]").isEqualTo("pdf")
				.jsonPath("$.owners.length()").isEqualTo(1)
				.jsonPath("$.owners[0]").isEqualTo("Visible owner")
				.jsonPath("$.sources.length()").isEqualTo(1);
	}

	private SearchFile copySearchFile(String fileName, String path) {
		SearchFile copy = new SearchFile();
		copy.setFileName(fileName);
		copy.setPath(path);
		copy.setServer(searchFile.getServer());
		copy.setCreatedDateTime(searchFile.getCreatedDateTime());
		copy.setLastModified(searchFile.getLastModified());
		copy.setAccessControlSource(searchFile.getAccessControlSource());
		copy.setIndexerUser(searchFile.getIndexerUser());
		copy.setIndexerReadable(searchFile.isIndexerReadable());
		copy.setFileOwner(searchFile.getFileOwner());
		copy.setOwnerReadable(searchFile.isOwnerReadable());
		copy.setSourceId(searchFile.getSourceId());
		copy.setSourceName(searchFile.getSourceName());
		copy.setSourceType(searchFile.getSourceType());
		copy.setSourceRoot(searchFile.getSourceRoot());
		copy.setRelativePath(fileName);
		copy.setContentOwner(searchFile.getContentOwner());
		copy.setOwnershipBasis(searchFile.getOwnershipBasis());
		copy.setDepartment(searchFile.getDepartment());
		return copy;
	}
}

package com.pereatech.volk.rest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.javafaker.Faker;
import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestTest {

	@Autowired
	private WebTestClient webTestClient;

	private SearchFile searchFile;

	private SearchUser createdBy;

	private Faker faker = new Faker();

	@Before
	public void setUp() {

		createdBy = new SearchUser();

		createdBy.setName(faker.name().fullName());
		createdBy.setDomainName(faker.ancient().god());

		searchFile = new SearchFile();
		searchFile.setFileName(faker.file().fileName());
		searchFile.setExtension(faker.file().extension());
		searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(), searchFile.getExtension(), "\\"));
		searchFile.setCreatedDateTime(LocalDateTime.now());
		searchFile.setServer(faker.gameOfThrones().dragon());
		searchFile.setLastModified(LocalDateTime.now());
		log.debug(searchFile);
		
	}

	@Test
	public void testSave() {
		webTestClient.post().uri("/searchfile").contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(searchFile), SearchFile.class).exchange()
				.expectStatus().isOk().expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8).expectBody()
				.jsonPath("$.fileName").isEqualTo(searchFile.getFileName());
	}

	@Test
	public void findAll() {
		webTestClient.post().uri("/searchfile/findall").contentType(MediaType.APPLICATION_JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON_UTF8).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON_UTF8).expectBodyList(SearchFile.class).returnResult()
				.getResponseBody().stream().forEach(searchFile -> log.debug(searchFile));
	}

}

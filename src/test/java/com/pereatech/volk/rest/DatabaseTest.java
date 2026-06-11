package com.pereatech.volk.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;

/**
 * Integration test — requires a MongoDB instance on localhost:27017
 * (see docker-compose.yml).
 */
@Slf4j
@SpringBootTest
class DatabaseTest {

	@Autowired
	SearchFileRepository searchFileRepository;

	@Autowired
	private SearchUserRepository searchUserRepository;

	private SearchFile searchFile;

	private SearchUser createdBy;

	private final Faker faker = new Faker();

	@BeforeEach
	void setUp() {
		createdBy = new SearchUser();
		createdBy.setName(faker.name().fullName());
		createdBy.setDomainName(faker.ancient().god());
		createdBy = searchUserRepository.save(createdBy).block();

		searchFile = new SearchFile();
		searchFile.setFileName(faker.file().fileName());
		searchFile.setExtension(faker.file().extension());
		searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(),
				searchFile.getExtension(), "\\"));
		searchFile.setCreatedDateTime(LocalDateTime.now());
		searchFile.setServer(faker.gameOfThrones().dragon());
		searchFile.setLastModified(LocalDateTime.now());
		searchFile = searchFileRepository.save(searchFile).block();

		log.debug("saved {}", searchFile);
	}

	@Test
	void findByFileNameReturnsSavedFile() {
		List<SearchFile> found = searchFileRepository.findByFileName(searchFile.getFileName()).collectList().block();

		assertThat(found).isNotEmpty();
		assertThat(found).allMatch(f -> f.getFileName().equals(searchFile.getFileName()));
	}
}

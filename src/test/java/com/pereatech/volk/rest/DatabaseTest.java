package com.pereatech.volk.rest;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.javafaker.Faker;
import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;

@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
public class DatabaseTest {
	@Autowired
	SearchFileRepository searchFileRepository;

	private SearchFile searchFile;

	private SearchUser createdBy;

	private Faker faker=new Faker();

	@Autowired
	private SearchUserRepository searchUserRepository;

	@Before
	public void setUp() {

		createdBy = new SearchUser();

		createdBy.setName(faker.name().fullName());
		createdBy.setDomainName(faker.ancient().god());
		
		createdBy = searchUserRepository.save(createdBy).block();

		searchFile = new SearchFile();
		searchFile.setFileName(faker.file().fileName());
		searchFile.setExtension(faker.file().extension());
		searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(), searchFile.getExtension(), "\\"));
		searchFile.setCreatedDateTime(LocalDateTime.now());
		searchFile.setServer(faker.gameOfThrones().dragon());
		searchFile.setLastModified(LocalDateTime.now());
		
		searchFile = searchFileRepository.save(searchFile).block();
		
		log.debug(searchFile);
	}

	@Test
	public void testLoad() {
		Flux<SearchFile> found = searchFileRepository.findByFileName(searchFile.getFileName());
		found.collectList().block().stream().forEach(a->log.debug(a));        
		assertThat(found).isNotNull();

	}

}

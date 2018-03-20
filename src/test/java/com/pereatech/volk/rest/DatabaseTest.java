package com.pereatech.volk.rest;
import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.test.context.junit4.SpringRunner;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
public class DatabaseTest {
	@Autowired
	SearchFileRepository repository;

	@Autowired
	ReactiveMongoOperations operations;

	private SearchFile searchFile;

	private SearchUser createdBy;

	@Before
	public void setUp() {

//		operations.collectionExists(SearchFile.class)
//				.flatMap(exists -> exists ? operations.dropCollection(SearchFile.class) : Mono.just(exists))
//				.flatMap(o -> operations.createCollection(SearchFile.class, new CollectionOptions(1024 * 1024, 100, true)))
//				.then().block();

		searchFile = new SearchFile();
		createdBy = new SearchUser();
		
		createdBy.setName("Armando");
		createdBy.setDomainName("mine");
		searchFile.setFileName("filename");
		searchFile.setPath("path");
		searchFile.setCreatedBy(createdBy);
		repository.save(searchFile)
				.then().block();
	}

	@Test
	public void testLoad() {
		Flux<SearchFile> found = repository.findByFileName(searchFile.getFileName());
		found.collectList().block().stream().forEach(a->log.debug(a));        
		assertThat(found).isNotNull();

	}

}

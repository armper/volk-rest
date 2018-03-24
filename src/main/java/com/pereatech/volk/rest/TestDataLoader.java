package com.pereatech.volk.rest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.github.javafaker.Faker;
import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class TestDataLoader implements CommandLineRunner {

	@Autowired
	SearchFileRepository searchFileRepository;

	private Faker faker = new Faker();

	@Autowired
	private SearchUserRepository searchUserRepository;

	@Override
	public void run(String... args) throws Exception {
		for (int i = 0; i < 50; i++) {
			SearchUser createdBy = new SearchUser();
			createdBy.setName(faker.name().fullName());
			createdBy.setDomainName(faker.ancient().god());
			createdBy.setSearchFiles(new ArrayList<>());

			for (int j = 0; j < faker.number().numberBetween(10, 100); j++) {
				SearchFile searchFile = new SearchFile();
				searchFile.setFileName(faker.file().fileName());
				searchFile.setExtension(faker.file().extension());
				searchFile.setPath(faker.file().fileName(faker.gameOfThrones().city(), searchFile.getFileName(),
						searchFile.getExtension(), "\\"));
				searchFile.setCreatedDateTime(LocalDateTime.now());
				searchFile.setServer(faker.gameOfThrones().dragon());
				searchFile.setLastModified(LocalDateTime.now());

				 searchFile = searchFileRepository.save(searchFile).block();
				createdBy.getSearchFiles().add(searchFile);
			}

			createdBy=searchUserRepository.save(createdBy).block();
		}
		
		log.debug("done");

	}
}
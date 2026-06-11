package com.pereatech.volk.rest.service;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.repositories.SearchFileRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/searchfile")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class SearchFileService {

	private final SearchFileRepository searchFileRepository;

	public SearchFileService(SearchFileRepository searchFileRepository) {
		this.searchFileRepository = searchFileRepository;
	}

	@GetMapping("/{id}")
	public Mono<SearchFile> findOne(@PathVariable("id") String id) {
		return searchFileRepository.findById(id);
	}

	@GetMapping("/findall")
	public Flux<SearchFile> findAll() {
		log.debug("findAll");
		return searchFileRepository.findAll();
	}

	@PostMapping
	public Mono<SearchFile> create(@RequestBody SearchFile searchFile) {
		log.debug("create {}", searchFile);
		return searchFileRepository.save(searchFile);
	}
}

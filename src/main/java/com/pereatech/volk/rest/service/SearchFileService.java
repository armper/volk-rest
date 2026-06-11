package com.pereatech.volk.rest.service;

import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.security.DocumentAccessService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/searchfile")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class SearchFileService {

	private final SearchFileRepository searchFileRepository;

	private final DocumentAccessService documentAccessService;

	public SearchFileService(SearchFileRepository searchFileRepository, DocumentAccessService documentAccessService) {
		this.searchFileRepository = searchFileRepository;
		this.documentAccessService = documentAccessService;
	}

	@GetMapping("/{id}")
	public Mono<SearchFile> findOne(@PathVariable("id") String id) {
		return searchFileRepository.findById(id)
				.filter(documentAccessService::canRead)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
	}

	@GetMapping("/")
	public Flux<SearchFile> findByUser(@RequestParam("userid") String userId) {
		return searchFileRepository.findByUserId(userId).filter(documentAccessService::canRead);
	}

	@GetMapping("/findall")
	public Flux<SearchFile> findAll() {
		return searchFileRepository.findAll().filter(documentAccessService::canRead);
	}

	/**
	 * Full-text search over file name, title, and extracted content.
	 */
	@GetMapping("/search")
	public Flux<SearchFile> search(@RequestParam("q") String query) {
		log.debug("text search: {}", query);
		return searchFileRepository.findAllBy(TextCriteria.forDefaultLanguage().matching(query))
				.filter(documentAccessService::canRead);
	}

	/**
	 * Saves a file. A file with the same path and server replaces the existing
	 * record instead of creating a duplicate.
	 */
	@PostMapping
	public Mono<SearchFile> create(@RequestBody SearchFile searchFile) {
		log.debug("create {}", searchFile);
		return searchFileRepository.findOneByPathAndServer(searchFile.getPath(), searchFile.getServer())
				.map(existing -> {
					searchFile.setId(existing.getId());
					return searchFile;
				})
				.defaultIfEmpty(searchFile)
				.flatMap(searchFileRepository::save);
	}
}

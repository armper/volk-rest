package com.pereatech.volk.rest.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/searchuser")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class SearchUserService {

	private final SearchUserRepository searchUserRepository;

	private final SearchFileRepository searchFileRepository;

	public SearchUserService(SearchUserRepository searchUserRepository, SearchFileRepository searchFileRepository) {
		this.searchUserRepository = searchUserRepository;
		this.searchFileRepository = searchFileRepository;
	}

	@GetMapping("/{id}")
	public Mono<SearchUser> findOne(@PathVariable("id") String id) {
		return searchUserRepository.findById(id);
	}

	@GetMapping("/findall")
	public Flux<SearchUser> findAll() {
		return searchUserRepository.findAll();
	}

	@GetMapping("/")
	public Flux<SearchUser> findByName(@RequestParam("name") String name) {
		return searchUserRepository.findByNameStartsWithIgnoringCase(name);
	}

	@PostMapping
	public Mono<ResponseEntity<String>> create(@RequestBody SearchUser searchUser) {
		return searchUserRepository
				.findOneByNameAndDomainName(searchUser.getName(), searchUser.getDomainName())
				.next()
				.map(existing -> {
					existing.getSearchFiles().addAll(searchUser.getSearchFiles());
					return existing;
				})
				.defaultIfEmpty(searchUser)
				.flatMap(searchUserRepository::save)
				.map(saved -> ResponseEntity.ok("resource saved"));
	}

	@PutMapping
	public Mono<ResponseEntity<String>> update(@RequestBody SearchUser searchUser) {
		return searchFileRepository.saveAll(searchUser.getSearchFiles())
				.collectList()
				.flatMap(files -> {
					searchUser.setSearchFiles(files);
					return searchUserRepository.save(searchUser);
				})
				.map(saved -> ResponseEntity.ok("resource saved"));
	}
}

package com.pereatech.volk.rest.service;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchUserRepository;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.security.DocumentAccessService;

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

	private final DocumentAccessService documentAccessService;

	public SearchUserService(SearchUserRepository searchUserRepository, SearchFileRepository searchFileRepository,
			DocumentAccessService documentAccessService) {
		this.searchUserRepository = searchUserRepository;
		this.searchFileRepository = searchFileRepository;
		this.documentAccessService = documentAccessService;
	}

	@GetMapping("/{id}")
	public Mono<SearchUser> findOne(@PathVariable("id") String id) {
		return searchUserRepository.findById(id)
				.filterWhen(user -> hasReadableFiles(user.getId()))
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
	}

	@GetMapping("/findall")
	public Flux<SearchUser> findAll() {
		return searchUserRepository.findAll().filterWhen(user -> hasReadableFiles(user.getId()));
	}

	@GetMapping("/")
	public Flux<SearchUser> findByName(@RequestParam("name") String name) {
		return searchUserRepository.findByNameStartsWithIgnoringCase(name)
				.filterWhen(user -> hasReadableFiles(user.getId()));
	}

	/**
	 * Get-or-create: returns the existing user with the same name and domain,
	 * or saves and returns the posted one. Always returns the persisted user
	 * including its id, so clients (the sniffer) can attribute files to it.
	 */
	@PostMapping
	public Mono<SearchUser> create(@RequestBody SearchUser searchUser) {
		return searchUserRepository
				.findOneByNameAndDomainName(searchUser.getName(), searchUser.getDomainName())
				.next()
				.switchIfEmpty(Mono.defer(() -> {
					log.debug("creating user {}", searchUser);
					return searchUserRepository.save(searchUser);
				}));
	}

	@PutMapping
	public Mono<SearchUser> update(@RequestBody SearchUser searchUser) {
		return searchUserRepository.save(searchUser);
	}

	private Mono<Boolean> hasReadableFiles(String userId) {
		return searchFileRepository.findByUserId(userId)
				.filter(documentAccessService::canRead)
				.hasElements();
	}
}

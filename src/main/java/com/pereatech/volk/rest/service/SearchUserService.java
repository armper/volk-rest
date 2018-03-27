package com.pereatech.volk.rest.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.repositories.SearchUserRepository;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
@RequestMapping("/searchuser")
@CrossOrigin(origins = "http://localhost:4200")
public class SearchUserService {

	@Autowired
	private SearchUserRepository searchUserRepository;

	@Autowired
	private SearchFileRepository searchFileRepository;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Mono<SearchUser> findOne(@PathVariable("id") String id) {
		return searchUserRepository.findById(id);
	}

	@RequestMapping(value = "/findall")
	public Flux<SearchUser> findAll() {
		return searchUserRepository.findAll();
	}

	@RequestMapping(value = "/")
	@ResponseBody
	public Flux<SearchUser> findByName(@RequestParam("name") String name) {
		return searchUserRepository.findByNameStartsWithIgnoringCase(name);
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> create(@RequestBody SearchUser searchUser) {
		SearchUser returnSearchUser = searchUserRepository
				.findOneByNameAndDomainName(searchUser.getName(), searchUser.getDomainName()).collectList().block()
				.stream().findFirst().orElse(searchUser);

		returnSearchUser.getSearchFiles().add(searchUser.getSearchFiles().stream().findFirst().orElse(null));

		returnSearchUser = searchUserRepository.save(returnSearchUser).block();
		log.debug("derp" + returnSearchUser);

		return ResponseEntity.ok("resource saved");
	}

	@RequestMapping(method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<String> update(@RequestBody SearchUser searchUser) {
		searchUser.setSearchFiles(searchFileRepository.saveAll(searchUser.getSearchFiles()).collectList().block());
		log.debug("derp" + searchUser);
		searchUser.getSearchFiles().stream().forEach(s -> log.debug(s));
		searchUserRepository.save(searchUser);

		return ResponseEntity.ok("resource saved");
	}
}

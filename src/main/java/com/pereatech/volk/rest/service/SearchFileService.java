package com.pereatech.volk.rest.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@RequestMapping("/searchfile")
@CrossOrigin(origins = "http://localhost:4200")
public class SearchFileService {

	@Autowired
	private SearchFileRepository searchFileRepository;

	@Autowired
	private SearchUserRepository searchUserRepository;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public Mono<SearchFile> findOne(@PathVariable("id") ObjectId id) {
		return searchFileRepository.findById(id);
	}

	@RequestMapping("/findall")
	public Flux<SearchFile> findAll() {
		log.debug("findAll");
		return searchFileRepository.findAll();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public Mono<SearchFile> create(@RequestBody SearchFile resource) {
		log.debug("create " + resource);

		SearchUser createdBy = resource.getCreatedBy();

		SearchUser foundUser = searchUserRepository
				.findOneByNameAndDomainName(createdBy.getName(), createdBy.getDomainName()).collectList().block()
				.stream().findFirst().orElse(null);
		
		if (foundUser==null)
		resource.setCreatedBy(searchUserRepository.save(resource.getCreatedBy()).block());

		return searchFileRepository.save(resource);
	}
}

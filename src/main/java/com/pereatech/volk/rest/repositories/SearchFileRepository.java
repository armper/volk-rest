package com.pereatech.volk.rest.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.model.SearchUser;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SearchFileRepository extends ReactiveCrudRepository<SearchFile, String> {
	
	public Flux<SearchFile> findByFileName(String fileName);

	public Mono<SearchFile> save(Mono<SearchFile> searchFile);

}

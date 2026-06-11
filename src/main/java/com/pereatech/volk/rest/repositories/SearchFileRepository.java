package com.pereatech.volk.rest.repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.pereatech.volk.rest.model.SearchFile;

import reactor.core.publisher.Flux;

public interface SearchFileRepository extends ReactiveCrudRepository<SearchFile, String> {

	Flux<SearchFile> findByFileName(String fileName);
}

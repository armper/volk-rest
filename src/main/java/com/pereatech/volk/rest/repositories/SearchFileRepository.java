package com.pereatech.volk.rest.repositories;

import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.pereatech.volk.rest.model.SearchFile;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SearchFileRepository extends ReactiveCrudRepository<SearchFile, String> {

	Flux<SearchFile> findByFileName(String fileName);

	Flux<SearchFile> findByUserId(String userId);

	Mono<SearchFile> findOneByPathAndServer(String path, String server);

	Flux<SearchFile> findAllBy(TextCriteria criteria);
}

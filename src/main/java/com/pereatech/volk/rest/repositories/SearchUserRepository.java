package com.pereatech.volk.rest.repositories;

import java.math.BigInteger;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.pereatech.volk.rest.model.SearchUser;

import reactor.core.publisher.Flux;


public interface SearchUserRepository extends ReactiveCrudRepository<SearchUser, String> {

	public Flux<SearchUser> findByNameStartsWithIgnoringCase(String name);

	public Flux<SearchUser> findOneByNameAndDomainName(String name, String domainName);
}

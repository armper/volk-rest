package com.pereatech.volk.rest.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@Document
@ToString
@EqualsAndHashCode(of = { "name", "domainName" })
public class SearchUser {
	@Id
	protected String id;
	
	private String name, domainName;
}

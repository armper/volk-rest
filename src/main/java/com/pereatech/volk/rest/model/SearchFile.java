package com.pereatech.volk.rest.model;


import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

@Data
@RequiredArgsConstructor
@Document
@ToString
@EqualsAndHashCode(of = { "fileName", "path", "server" })
public class SearchFile {

	@Id
	protected ObjectId id;
	
	protected SearchUser createdBy;

	protected Long size;
	
	protected LocalDateTime createdDateTime, lastModified;

	protected String fileName, path, extension, server, share;
}

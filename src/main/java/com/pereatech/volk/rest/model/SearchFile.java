package com.pereatech.volk.rest.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@Document
@ToString(exclude = "content")
@EqualsAndHashCode(of = { "fileName", "path", "server" })
public class SearchFile {

	@Id
	protected String id;

	/** Owning SearchUser. */
	@Indexed
	protected String userId;

	protected Long size;

	protected LocalDateTime createdDateTime, lastModified;

	@TextIndexed(weight = 3)
	protected String fileName;

	protected String path, extension, server, share;

	/** Document metadata extracted by the sniffer. */
	@TextIndexed(weight = 2)
	protected String title;

	protected String author, keywords, comments, contentType;

	/** Human and system provenance inherited from the watched source. */
	protected String sourceId, sourceType, sourceRoot, relativePath, ownershipBasis, accessContextRoot,
			sourceAccessSummary;

	protected String sourceName, contentOwner, department;

	/** Filesystem access metadata captured by the sniffer. */
	protected String fileOwner, fileGroup, posixPermissions, accessControlSource, indexerUser;

	protected boolean ownerReadable, groupReadable, othersReadable, indexerReadable;

	protected List<String> allowedPrincipals = new ArrayList<>();

	protected List<String> deniedPrincipals = new ArrayList<>();

	/**
	 * Extracted text content. Accepted on ingest but never serialized back out
	 * — it is only there to power full-text search.
	 */
	@TextIndexed
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	protected String content;
}

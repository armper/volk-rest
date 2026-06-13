package com.pereatech.volk.rest.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.security.DocumentAccessService;
import com.pereatech.volk.rest.service.DocumentPreviewService.DocumentPreview;
import com.pereatech.volk.rest.service.DocumentPreviewService.PreviewResource;
import com.pereatech.volk.rest.service.DocumentSearchService.SearchOptions;
import com.pereatech.volk.rest.service.DocumentSearchService.SearchRequest;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/searchfile")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class SearchFileService {

	private final SearchFileRepository searchFileRepository;

	private final DocumentAccessService documentAccessService;

	private final DocumentSearchService documentSearchService;

	private final DocumentPreviewService documentPreviewService;

	public SearchFileService(SearchFileRepository searchFileRepository, DocumentAccessService documentAccessService,
			DocumentSearchService documentSearchService, DocumentPreviewService documentPreviewService) {
		this.searchFileRepository = searchFileRepository;
		this.documentAccessService = documentAccessService;
		this.documentSearchService = documentSearchService;
		this.documentPreviewService = documentPreviewService;
	}

	@GetMapping("/{id}")
	public Mono<SearchFile> findOne(@PathVariable("id") String id) {
		return searchFileRepository.findById(id)
				.filter(documentAccessService::canRead)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
	}

	@GetMapping("/")
	public Flux<SearchFile> findByUser(@RequestParam("userid") String userId) {
		return searchFileRepository.findByUserId(userId).filter(documentAccessService::canRead);
	}

	@GetMapping("/findall")
	public Flux<SearchFile> findAll() {
		return searchFileRepository.findAll().filter(documentAccessService::canRead);
	}

	/**
	 * Permission-aware text search, filtering, and sorting.
	 */
	@GetMapping("/search")
	public Flux<SearchFile> search(
			@RequestParam(name = "q", defaultValue = "") String query,
			@RequestParam(required = false) String extension,
			@RequestParam(required = false) String owner,
			@RequestParam(required = false) String sourceId,
			@RequestParam(required = false) String folder,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modifiedFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate modifiedTo,
			@RequestParam(required = false) Long minSize,
			@RequestParam(required = false) Long maxSize,
			@RequestParam(required = false) String author,
			@RequestParam(required = false) String keywords,
			@RequestParam(defaultValue = "relevance") String sort) {
		log.debug("document search: {}", query);
		return documentSearchService.search(new SearchRequest(query, extension, owner, sourceId, folder,
				modifiedFrom, modifiedTo, minSize, maxSize, author, keywords, sort));
	}

	@GetMapping("/search/options")
	public Mono<SearchOptions> searchOptions() {
		return documentSearchService.options();
	}

	@GetMapping("/{id}/preview")
	public Mono<DocumentPreview> preview(@PathVariable String id) {
		return documentPreviewService.preview(id);
	}

	@GetMapping("/{id}/content")
	public Mono<ResponseEntity<Resource>> previewContent(@PathVariable String id) {
		return documentPreviewService.content(id).map(this::inlineResponse);
	}

	private ResponseEntity<Resource> inlineResponse(PreviewResource preview) {
		ContentDisposition disposition = ContentDisposition.inline()
				.filename(preview.fileName(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.contentType(preview.mediaType())
				.contentLength(preview.contentLength())
				.cacheControl(CacheControl.noStore())
				.header("Content-Disposition", disposition.toString())
				.header("X-Content-Type-Options", "nosniff")
				.body(preview.resource());
	}

	/**
	 * Saves a file. A file with the same path and server replaces the existing
	 * record instead of creating a duplicate.
	 */
	@PostMapping
	public Mono<SearchFile> create(@RequestBody SearchFile searchFile) {
		log.debug("create {}", searchFile);
		return searchFileRepository.findOneByPathAndServer(searchFile.getPath(), searchFile.getServer())
				.map(existing -> {
					searchFile.setId(existing.getId());
					return searchFile;
				})
				.defaultIfEmpty(searchFile)
				.flatMap(searchFileRepository::save);
	}
}

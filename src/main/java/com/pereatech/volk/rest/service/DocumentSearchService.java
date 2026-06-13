package com.pereatech.volk.rest.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.security.DocumentAccessService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DocumentSearchService {

	private static final int MAX_RESULTS = 500;

	private final ReactiveMongoTemplate mongoTemplate;

	private final SearchFileRepository searchFileRepository;

	private final DocumentAccessService documentAccessService;

	public DocumentSearchService(ReactiveMongoTemplate mongoTemplate, SearchFileRepository searchFileRepository,
			DocumentAccessService documentAccessService) {
		this.mongoTemplate = mongoTemplate;
		this.searchFileRepository = searchFileRepository;
		this.documentAccessService = documentAccessService;
	}

	public Flux<SearchFile> search(SearchRequest request) {
		Query query = textQuery(request.query());
		addExact(query, "extension", lower(request.extension()));
		addExact(query, "contentOwner", request.owner());
		addExact(query, "sourceId", request.sourceId());
		addContains(query, "author", request.author());
		addContains(query, "keywords", request.keywords());
		addFolder(query, request.folder());
		addDates(query, request.modifiedFrom(), request.modifiedTo());
		addSize(query, request.minSize(), request.maxSize());
		applySort(query, request.sort(), hasText(request.query()));
		query.limit(MAX_RESULTS);

		return mongoTemplate.find(query, SearchFile.class).filter(documentAccessService::canRead);
	}

	public Mono<SearchOptions> options() {
		return searchFileRepository.findAll()
				.filter(documentAccessService::canRead)
				.collectList()
				.map(files -> new SearchOptions(
						distinct(files.stream().map(SearchFile::getExtension).toList()),
						distinct(files.stream().map(SearchFile::getContentOwner).toList()),
						distinct(files.stream().map(SearchFile::getAuthor).toList()),
						sources(files),
						files.stream().map(SearchFile::getSize).filter(Objects::nonNull).max(Long::compareTo).orElse(0L)));
	}

	private Query textQuery(String query) {
		if (!hasText(query)) {
			return new Query();
		}
		return TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(query.trim()));
	}

	private void addExact(Query query, String field, String value) {
		if (hasText(value)) {
			query.addCriteria(Criteria.where(field).regex(exactPattern(value)));
		}
	}

	private void addContains(Query query, String field, String value) {
		if (hasText(value)) {
			query.addCriteria(Criteria.where(field).regex(containsPattern(value)));
		}
	}

	private void addFolder(Query query, String folder) {
		if (!hasText(folder)) {
			return;
		}
		Pattern pattern = containsPattern(folder);
		query.addCriteria(new Criteria().orOperator(
				Criteria.where("relativePath").regex(pattern),
				Criteria.where("sourceRoot").regex(pattern),
				Criteria.where("path").regex(pattern)));
	}

	private void addDates(Query query, LocalDate from, LocalDate to) {
		if (from == null && to == null) {
			return;
		}
		Criteria dates = Criteria.where("lastModified");
		if (from != null) {
			dates.gte(from.atStartOfDay());
		}
		if (to != null) {
			dates.lt(to.plusDays(1).atStartOfDay());
		}
		query.addCriteria(dates);
	}

	private void addSize(Query query, Long minSize, Long maxSize) {
		if (minSize == null && maxSize == null) {
			return;
		}
		Criteria size = Criteria.where("size");
		if (minSize != null) {
			size.gte(Math.max(0, minSize));
		}
		if (maxSize != null) {
			size.lte(Math.max(0, maxSize));
		}
		query.addCriteria(size);
	}

	private void applySort(Query query, String requestedSort, boolean hasQuery) {
		String sort = hasText(requestedSort) ? requestedSort : "relevance";
		switch (sort) {
		case "relevance" -> {
			if (hasQuery && query instanceof TextQuery textQuery) {
				textQuery.sortByScore();
			}
			query.with(Sort.by(Sort.Direction.DESC, "lastModified"));
		}
		case "newest" -> query.with(Sort.by(Sort.Direction.DESC, "lastModified"));
		case "oldest" -> query.with(Sort.by(Sort.Direction.ASC, "lastModified"));
		case "name" -> query.with(Sort.by(Sort.Direction.ASC, "fileName"));
		case "size-desc" -> query.with(Sort.by(Sort.Direction.DESC, "size", "lastModified"));
		case "size-asc" -> query.with(Sort.by(Sort.Direction.ASC, "size"));
		case "owner" -> query.with(Sort.by(Sort.Direction.ASC, "contentOwner"));
		default -> query.with(Sort.by(Sort.Direction.DESC, "lastModified"));
		}
	}

	private List<String> distinct(List<String> values) {
		return values.stream()
				.filter(this::hasText)
				.distinct()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
	}

	private List<SearchSourceOption> sources(List<SearchFile> files) {
		return files.stream()
				.filter(file -> hasText(file.getSourceId()) && hasText(file.getSourceName()))
				.map(file -> new SearchSourceOption(file.getSourceId(), file.getSourceName(), file.getSourceType()))
				.distinct()
				.sorted(Comparator.comparing(SearchSourceOption::name, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private Pattern exactPattern(String value) {
		return Pattern.compile("^" + Pattern.quote(value.trim()) + "$", Pattern.CASE_INSENSITIVE);
	}

	private Pattern containsPattern(String value) {
		return Pattern.compile(Pattern.quote(value.trim()), Pattern.CASE_INSENSITIVE);
	}

	private String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public record SearchRequest(String query, String extension, String owner, String sourceId, String folder,
			LocalDate modifiedFrom, LocalDate modifiedTo, Long minSize, Long maxSize, String author, String keywords,
			String sort) { }

	public record SearchOptions(List<String> extensions, List<String> owners, List<String> authors,
			List<SearchSourceOption> sources, long maxSize) { }

	public record SearchSourceOption(String id, String name, String type) { }
}

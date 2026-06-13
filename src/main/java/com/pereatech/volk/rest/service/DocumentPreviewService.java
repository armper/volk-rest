package com.pereatech.volk.rest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.repositories.SearchFileRepository;
import com.pereatech.volk.rest.security.DocumentAccessService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class DocumentPreviewService {

	private static final int PREVIEW_TEXT_LIMIT = 50_000;

	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"bmp", "gif", "jpeg", "jpg", "png", "tif", "tiff", "webp");

	private static final Map<String, MediaType> IMAGE_MEDIA_TYPES = Map.of(
			"bmp", MediaType.parseMediaType("image/bmp"),
			"gif", MediaType.IMAGE_GIF,
			"jpeg", MediaType.IMAGE_JPEG,
			"jpg", MediaType.IMAGE_JPEG,
			"png", MediaType.IMAGE_PNG,
			"tif", MediaType.parseMediaType("image/tiff"),
			"tiff", MediaType.parseMediaType("image/tiff"),
			"webp", MediaType.parseMediaType("image/webp"));

	private final SearchFileRepository searchFileRepository;

	private final DocumentAccessService documentAccessService;

	public DocumentPreviewService(SearchFileRepository searchFileRepository,
			DocumentAccessService documentAccessService) {
		this.searchFileRepository = searchFileRepository;
		this.documentAccessService = documentAccessService;
	}

	public Mono<DocumentPreview> preview(String id) {
		return readableFile(id).flatMap(file -> Mono.fromCallable(() -> buildPreview(file))
				.subscribeOn(Schedulers.boundedElastic()));
	}

	public Mono<PreviewResource> content(String id) {
		return readableFile(id).flatMap(file -> Mono.fromCallable(() -> buildResource(file))
				.subscribeOn(Schedulers.boundedElastic()));
	}

	private Mono<SearchFile> readableFile(String id) {
		return searchFileRepository.findById(id)
				.filter(documentAccessService::canRead)
				.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
	}

	private DocumentPreview buildPreview(SearchFile file) {
		String text = cleanText(file.getContent());
		boolean textTruncated = text != null && text.length() > PREVIEW_TEXT_LIMIT;
		if (textTruncated) {
			text = text.substring(0, PREVIEW_TEXT_LIMIT).stripTrailing();
		}

		boolean originalAvailable = isInlineType(file) && resolveReadablePath(file) != null;
		String previewType = originalAvailable ? inlineType(file) : text != null ? "TEXT" : "UNAVAILABLE";
		return new DocumentPreview(file.getId(), file.getFileName(), file.getTitle(), file.getExtension(),
				file.getContentType(), previewType, text, textTruncated, originalAvailable);
	}

	private PreviewResource buildResource(SearchFile file) {
		if (!isInlineType(file)) {
			throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		}
		Path path = resolveReadablePath(file);
		if (path == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		try {
			return new PreviewResource(new FileSystemResource(path), mediaType(file), Files.size(path), file.getFileName());
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
	}

	private Path resolveReadablePath(SearchFile file) {
		if (!hasText(file.getPath()) || !hasText(file.getSourceRoot())) {
			return null;
		}
		try {
			Path sourceRoot = Path.of(file.getSourceRoot()).toRealPath();
			Path filePath = Path.of(file.getPath()).toRealPath();
			if (!filePath.startsWith(sourceRoot) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
				return null;
			}
			return filePath;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	private boolean isInlineType(SearchFile file) {
		return "PDF".equals(inlineType(file)) || "IMAGE".equals(inlineType(file));
	}

	private String inlineType(SearchFile file) {
		String extension = lower(file.getExtension());
		String contentType = lower(file.getContentType());
		if ("pdf".equals(extension) || contentType.startsWith(MediaType.APPLICATION_PDF_VALUE)) {
			return "PDF";
		}
		if (IMAGE_EXTENSIONS.contains(extension) || IMAGE_MEDIA_TYPES.values().stream()
				.anyMatch(type -> type.isCompatibleWith(parseMediaType(contentType)))) {
			return "IMAGE";
		}
		return "";
	}

	private MediaType mediaType(SearchFile file) {
		if ("PDF".equals(inlineType(file))) {
			return MediaType.APPLICATION_PDF;
		}
		MediaType extensionType = IMAGE_MEDIA_TYPES.get(lower(file.getExtension()));
		return extensionType != null ? extensionType : parseMediaType(file.getContentType());
	}

	private MediaType parseMediaType(String value) {
		try {
			return hasText(value) ? MediaType.parseMediaType(value) : MediaType.APPLICATION_OCTET_STREAM;
		} catch (IllegalArgumentException e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private String cleanText(String value) {
		if (!hasText(value)) {
			return null;
		}
		return value.replace("\u0000", "").strip();
	}

	private String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public record DocumentPreview(String id, String fileName, String title, String extension, String contentType,
			String previewType, String text, boolean textTruncated, boolean originalAvailable) { }

	public record PreviewResource(Resource resource, MediaType mediaType, long contentLength, String fileName) { }
}

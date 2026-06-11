package com.pereatech.volk.rest.security;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pereatech.volk.rest.model.SearchFile;

@Service
public class DocumentAccessService {

	private final ViewerIdentityService viewerIdentityService;

	public DocumentAccessService(ViewerIdentityService viewerIdentityService) {
		this.viewerIdentityService = viewerIdentityService;
	}

	public boolean canRead(SearchFile file) {
		if (viewerIdentityService.current().admin()) {
			return true;
		}
		if (!hasAccessMetadata(file) || matchesAny(file.getDeniedPrincipals())) {
			return false;
		}
		if (viewerIdentityService.isUser(file.getIndexerUser())) {
			return file.isIndexerReadable();
		}
		if (matchesAny(file.getAllowedPrincipals())) {
			return true;
		}
		if (viewerIdentityService.isUser(file.getFileOwner())) {
			return file.isOwnerReadable();
		}
		if (viewerIdentityService.matches(file.getFileGroup())) {
			return file.isGroupReadable();
		}
		return file.isOthersReadable();
	}

	private boolean hasAccessMetadata(SearchFile file) {
		return file.getAccessControlSource() != null && !file.getAccessControlSource().isBlank();
	}

	private boolean matchesAny(List<String> principals) {
		return principals != null && principals.stream().anyMatch(viewerIdentityService::matches);
	}
}

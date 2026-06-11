package com.pereatech.volk.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.pereatech.volk.rest.model.SearchFile;
import com.pereatech.volk.rest.security.DocumentAccessService;
import com.pereatech.volk.rest.security.ViewerIdentityService;

class DocumentAccessServiceTest {

	private final ViewerIdentityService identity = new ViewerIdentityService("alice", "engineering,staff", "");

	private final DocumentAccessService access = new DocumentAccessService(identity);

	@Test
	void allowsTheIndexerOnlyWhenTheIndexerCouldReadTheFile() {
		SearchFile readable = protectedFile();
		readable.setIndexerUser("alice");
		readable.setIndexerReadable(true);
		assertThat(access.canRead(readable)).isTrue();

		readable.setIndexerReadable(false);
		assertThat(access.canRead(readable)).isFalse();
	}

	@Test
	void honorsOwnerGroupOtherAndExplicitDenials() {
		SearchFile file = protectedFile();
		file.setFileOwner("bob");
		file.setFileGroup("engineering");
		file.setGroupReadable(true);
		assertThat(access.canRead(file)).isTrue();

		file.setDeniedPrincipals(List.of("alice"));
		assertThat(access.canRead(file)).isFalse();
	}

	@Test
	void failsClosedWhenPermissionMetadataIsMissing() {
		assertThat(access.canRead(new SearchFile())).isFalse();
	}

	private SearchFile protectedFile() {
		SearchFile file = new SearchFile();
		file.setAccessControlSource("POSIX");
		return file;
	}
}

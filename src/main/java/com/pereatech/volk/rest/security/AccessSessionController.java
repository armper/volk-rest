package com.pereatech.volk.rest.security;

import java.util.Set;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/access")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class AccessSessionController {

	private final ViewerIdentityService viewerIdentityService;

	public AccessSessionController(ViewerIdentityService viewerIdentityService) {
		this.viewerIdentityService = viewerIdentityService;
	}

	@GetMapping("/session")
	public AccessSession session() {
		var identity = viewerIdentityService.current();
		return new AccessSession(identity.user(), identity.groups(), identity.admin(), "FILESYSTEM_PERMISSIONS");
	}

	public record AccessSession(String user, Set<String> groups, boolean admin, String enforcement) { }
}

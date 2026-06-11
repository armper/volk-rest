package com.pereatech.volk.rest.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ViewerIdentityService {

	private final ViewerIdentity identity;

	public ViewerIdentityService(
			@Value("${volk.access.user:${user.name}}") String user,
			@Value("${volk.access.groups:}") String configuredGroups,
			@Value("${volk.access.admin-users:}") String configuredAdmins) {
		Set<String> groups = configuredGroups.isBlank() ? resolveOsGroups(user) : split(configuredGroups);
		Set<String> admins = split(configuredAdmins);
		this.identity = new ViewerIdentity(user, groups, containsPrincipal(admins, user));
	}

	public ViewerIdentity current() {
		return identity;
	}

	public Set<String> principals() {
		Set<String> principals = new LinkedHashSet<>();
		principals.addAll(aliases(identity.user()));
		identity.groups().forEach(group -> principals.addAll(aliases(group)));
		return principals;
	}

	public boolean matches(String principal) {
		if (principal == null || principal.isBlank()) {
			return false;
		}
		Set<String> candidateAliases = aliases(principal);
		return principals().stream().anyMatch(candidateAliases::contains);
	}

	public boolean isUser(String principal) {
		return principal != null && aliases(identity.user()).stream().anyMatch(aliases(principal)::contains);
	}

	private Set<String> resolveOsGroups(String user) {
		try {
			Process process = new ProcessBuilder("id", "-Gn", user).redirectErrorStream(true).start();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line = reader.readLine();
				if (process.waitFor() == 0 && line != null) {
					return split(line.replace(' ', ','));
				}
			}
		} catch (IOException e) {
			// The configured identity still enforces owner and explicit ACL access.
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return Set.of();
	}

	private Set<String> split(String value) {
		Set<String> values = new LinkedHashSet<>();
		Arrays.stream(value.split("[,\\s]+"))
				.map(String::trim)
				.filter(item -> !item.isBlank())
				.forEach(values::add);
		return Set.copyOf(values);
	}

	private boolean containsPrincipal(Set<String> principals, String user) {
		Set<String> userAliases = aliases(user);
		return principals.stream().flatMap(principal -> aliases(principal).stream()).anyMatch(userAliases::contains);
	}

	private Set<String> aliases(String principal) {
		String normalized = principal.trim().toLowerCase(Locale.ROOT);
		Set<String> aliases = new LinkedHashSet<>();
		aliases.add(normalized);
		int slash = Math.max(normalized.lastIndexOf('\\'), normalized.lastIndexOf('/'));
		if (slash >= 0 && slash + 1 < normalized.length()) {
			aliases.add(normalized.substring(slash + 1));
		}
		int at = normalized.indexOf('@');
		if (at > 0) {
			aliases.add(normalized.substring(0, at));
		}
		return aliases;
	}

	public record ViewerIdentity(String user, Set<String> groups, boolean admin) { }
}

package com.pereatech.volk.rest.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Optional API-key auth. When {@code volk.security.api-key} is set, every
 * request must carry it in the {@code X-API-Key} header. CORS preflights and
 * the health endpoint stay open. When the property is empty (the default for
 * local development), the filter is a no-op.
 */
@Component
public class ApiKeyFilter implements WebFilter {

	public static final String API_KEY_HEADER = "X-API-Key";

	@Value("${volk.security.api-key:}")
	private String apiKey;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (apiKey.isBlank() || isOpen(exchange) || isAuthorized(exchange)) {
			return chain.filter(exchange);
		}

		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
		return exchange.getResponse().setComplete();
	}

	private boolean isOpen(ServerWebExchange exchange) {
		return HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
				|| exchange.getRequest().getPath().value().startsWith("/actuator/health");
	}

	private boolean isAuthorized(ServerWebExchange exchange) {
		return apiKey.equals(exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER));
	}
}

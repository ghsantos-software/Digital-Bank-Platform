package com.digitalbank.gateway.config;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every request carries an X-Correlation-ID header, generating one if absent.
 * Uses ServerHttpRequestDecorator to inject the header without touching the read-only
 * backing map (DefaultServerHttpRequestBuilder.header() calls put() on ReadOnlyHttpHeaders
 * which throws UnsupportedOperationException in Spring Security contexts).
 */
@Component
@Order(-1)
public class CorrelationIdFilter implements WebFilter {

    static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Decorate the request: override getHeaders() to return a mutable copy that
        // includes the correlation ID — avoids any put() call on ReadOnlyHttpHeaders.
        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.addAll(super.getHeaders());
                headers.set(CORRELATION_ID_HEADER, finalCorrelationId);
                return headers;
            }
        };

        ServerWebExchange mutated = exchange.mutate()
                .request(decoratedRequest)
                .build();

        return chain.filter(mutated)
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_HEADER, finalCorrelationId));
    }
}

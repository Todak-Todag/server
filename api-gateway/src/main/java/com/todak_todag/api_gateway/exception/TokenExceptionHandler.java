package com.todak_todag.api_gateway.exception;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import com.todak_todag.api_gateway.response.TokenErrorResponse;

import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(-2)
public class TokenExceptionHandler implements WebExceptionHandler {

	private final JsonMapper jsonMapper;

	public TokenExceptionHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}
	
	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		if(!(ex instanceof TokenException tokenException)) {
			return Mono.error(ex);
		}
		
		ServerHttpResponse response = exchange.getResponse();
		
		if(response.isCommitted()) {
			return Mono.error(ex);
		}
		
		TokenErrorCode errorCode = tokenException.getErrorCode();
		
		TokenErrorResponse errorResponse = TokenErrorResponse.from(errorCode);
		
		byte[] responseBody;
		
		try {
			responseBody = jsonMapper.writeValueAsBytes(errorResponse);
		} catch (JacksonException e) {
			return Mono.error(e);
		}
		
		response.setStatusCode(errorCode.getStatus());
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		
		DataBuffer buffer = response.bufferFactory()
				.wrap(responseBody);
		
		return response.writeWith(Mono.just(buffer));
	}
}

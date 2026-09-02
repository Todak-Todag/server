package com.todak_todag.user_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.security.InternalHeader;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Internal API 통합테스트")
public class InternalApiSecurityIntegrationTest {

	private static final String INTERNAL_API_KEY = "01234567890123456789012345678901";
	
	@Autowired
	private MockMvc mockMvc;
	
	@TestConfiguration
	static class TestInternalEndPoint {
		
		@RestController
		@RequestMapping("/internal/v1/probe")
		static class ProbeController {
			
			@GetMapping
			String probe() {
				return "probe";
			}
			
		}
		
	}
	
	@Test
	@DisplayName("유효한 내부 키로 호출하면 통과")
	void internalApiTest_1() throws Exception {
		mockMvc.perform(
				get("/internal/v1/probe")
				.header(InternalHeader.INTERNAL_API_KEY, INTERNAL_API_KEY)
		)
		.andExpect(status().isOk());
	}
	
}

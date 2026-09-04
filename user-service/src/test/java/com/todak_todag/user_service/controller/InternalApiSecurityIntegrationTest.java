package com.todak_todag.user_service.controller;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.security.InternalHeader;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Internal API 통합테스트")
public class InternalApiSecurityIntegrationTest {

	private static final String INTERNAL_API_KEY = "01234567890123456789012345678901";
	
	private static final String INTERNAL_TEST_URL = "/internal/v1/probe";
	
	private static final String ERROR_CODE = CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST.getCode();
	
	@Autowired
	private MockMvc mockMvc;
	
	@TestConfiguration
	static class TestInternalEndPoint {
		
		@RestController
		@RequestMapping(INTERNAL_TEST_URL)
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
				get(INTERNAL_TEST_URL)
				.header(InternalHeader.INTERNAL_KEY, INTERNAL_API_KEY)
		)
		.andExpect(status().isOk());
	}
	
	@Test
	@DisplayName("내부 키 없이 호출하면 401 응답")
	void internalApiTest_fail_1() throws Exception {
		mockMvc.perform(
				get(INTERNAL_TEST_URL)
		)
		.andExpect(status().isUnauthorized())
		.andExpect(jsonPath("$.success").value(false))
		.andExpect(jsonPath("$.error.errorCode").value(ERROR_CODE));
	}
	
	@Test
	@DisplayName("잘못된 내부 키로 호출하면 401 응답")
	void internalApiTest_fail_2() throws Exception {
		mockMvc.perform(
				get(INTERNAL_TEST_URL)
				.header(InternalHeader.INTERNAL_KEY, "fail_internal_test_key")
		)
		.andExpect(status().isUnauthorized())
		.andExpect(jsonPath("$.success").value(false))
		.andExpect(jsonPath("$.error.errorCode").value(ERROR_CODE));
	}
	
	@Test
	@DisplayName("공개 API는 내부 키 없이도 인터셉터를 거치지 않아야함")
	void internalApiTest_isPublicApi() throws Exception {
		mockMvc.perform(get("/api/v1/probe"))
		.andExpect(status().is(not(401)));
	}
	
}

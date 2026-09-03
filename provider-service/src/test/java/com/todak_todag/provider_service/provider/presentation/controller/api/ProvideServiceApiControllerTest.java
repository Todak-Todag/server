package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.config.SecurityConfig;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideServiceCommandService;
import com.todak_todag.provider_service.provider.application.service.query.ProvideServiceQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProvideServiceApiController.class)
@DisplayName("서비스 종류 API")
class ProvideServiceApiControllerTest {

    private static final String BASE_URL = "/api/v1/provide-services";
    private static final String NAME = "방문간호";
    private static final String CONTENT = "간호사가 가정을 방문해 간호 서비스를 제공합니다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvideServiceCommandService provideServiceCommandService;

    @MockitoBean
    private ProvideServiceQueryService provideServiceQueryService;


    @Nested
    @DisplayName("등록")
    class Create {

        private String body(String name, String content) {
            return """
                    { "name": "%s", "content": "%s" }
                    """.formatted(name, content);
        }

        @Test
        @DisplayName("MASTER가 등록에 성공하면 201과 provideServiceId를 반환한다")
        void create_success() throws Exception {
            UUID provideServiceId = UUID.randomUUID();

            given(provideServiceCommandService.create(any()))
                    .willReturn(new ProvideServiceCreateResult(provideServiceId, NAME, CONTENT));

            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "MASTER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NAME, CONTENT)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.message").value("서비스 종류 등록 성공"))
                    .andExpect(jsonPath("$.data.provideServiceId").value(provideServiceId.toString()))
                    .andExpect(jsonPath("$.data.name").value(NAME));
        }

        @Test
        @DisplayName("이미 등록된 서비스명이면 409를 반환한다")
        void create_duplicateName_conflict() throws Exception {
            given(provideServiceCommandService.create(any()))
                    .willThrow(new BusinessException(ProviderErrorCode.PROVIDE_SERVICE_DUPLICATE));

            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "MASTER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NAME, CONTENT)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PROVIDE_SERVICE_DUPLICATE"));
        }

        @Test
        @DisplayName("ADMIN이 요청하면 403을 반환한다")
        void create_admin_forbidden() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NAME, CONTENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("SERVICE_PROVIDER가 요청하면 403을 반환한다")
        void create_serviceProvider_forbidden() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NAME, CONTENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 헤더가 없으면 403을 반환한다")
        void create_noAuth_forbidden() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(NAME, CONTENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("서비스명이 비어 있으면 400을 반환한다")
        void create_blankName_badRequest() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "MASTER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("", CONTENT)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("서비스명이 50자를 넘으면 400을 반환한다")
        void create_tooLongName_badRequest() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "MASTER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("가".repeat(51), CONTENT)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    @Nested
    @DisplayName("목록 조회")
    class Search {

        @Test
        @DisplayName("인증된 사용자면 200과 목록을 반환한다")
        void search_success() throws Exception {
            UUID provideServiceId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-08-01T09:00:00Z");
            Page<ProvideServiceSearchResult> page = new PageImpl<>(
                    List.of(new ProvideServiceSearchResult(provideServiceId, NAME, CONTENT, createdAt)),
                    PageRequest.of(0, 10), 1);

            given(provideServiceQueryService.search(any())).willReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("서비스 종류 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].provideServiceId").value(provideServiceId.toString()))
                    .andExpect(jsonPath("$.data.content[0].provideServiceName").value(NAME))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1));
        }

        @Test
        @DisplayName("MASTER가 아니어도 조회할 수 있다")
        void search_anyRole_success() throws Exception {
            given(provideServiceQueryService.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("인증 헤더가 없으면 403을 반환한다")
        void search_noAuth_forbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("결과가 없으면 200과 빈 목록을 반환한다")
        void search_empty() throws Exception {
            given(provideServiceQueryService.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(BASE_URL)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0));
        }
    }
}
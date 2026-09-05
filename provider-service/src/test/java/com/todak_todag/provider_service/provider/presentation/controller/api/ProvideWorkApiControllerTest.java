package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.config.SecurityConfig;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.service.command.ProvideWorkCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ProvideWorkApiController.class)
@DisplayName("제공 가능 일정 API")
class ProvideWorkApiControllerTest {

    private static final String BASE_URL = "/api/v1/service-offerings/{serviceOfferingId}/provide-works";

    private final UUID serviceOfferingId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvideWorkCommandService provideWorkCommandService;

    private String body(String day, String startedAt, String finishedAt) {
        return """
                { "day": %s, "startedAt": "%s", "finishedAt": "%s" }
                """.formatted(day, startedAt, finishedAt);
    }

    @Test
    @DisplayName("등록에 성공하면 201과 provideWorkId를 반환한다")
    void create_success() throws Exception {
        UUID provideWorkId = UUID.randomUUID();

        given(provideWorkCommandService.create(any()))
                .willReturn(new ProvideWorkCreateResult(provideWorkId));

        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.provideWorkId").value(provideWorkId.toString()));
    }

    @Test
    @DisplayName("SERVICE_PROVIDER가 아니면 403")
    void create_forbidden() throws Exception {
        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("인증 헤더가 없으면 403")
    void create_noAuthHeader() throws Exception {
        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("본인 소유가 아니면 403")
    void create_notOwner() throws Exception {
        given(provideWorkCommandService.create(any()))
                .willThrow(new BusinessException(ProviderErrorCode.AUTH_FORBIDDEN));

        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("존재하지 않는 제공 서비스면 404")
    void create_notFound() throws Exception {
        given(provideWorkCommandService.create(any()))
                .willThrow(new BusinessException(ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND));

        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_OFFERING_NOT_FOUND"));
    }

    @Test
    @DisplayName("시간이 겹치면 409")
    void create_timeOverlap() throws Exception {
        given(provideWorkCommandService.create(any()))
                .willThrow(new BusinessException(ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP));

        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROVIDE_WORK_TIME_OVERLAP"));
    }

    @Test
    @DisplayName("day가 범위를 벗어나면 400")
    void create_invalidDay() throws Exception {
        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("8", "09:00", "13:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("필수 값이 없으면 400")
    void create_missingField() throws Exception {
        mockMvc.perform(post(BASE_URL, serviceOfferingId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "day": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("serviceOfferingId가 UUID 형식이 아니면 400")
    void create_invalidServiceOfferingId() throws Exception {
        mockMvc.perform(post(BASE_URL, "not-a-uuid")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "09:00", "13:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.config.SecurityConfig;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.facade.ServiceOfferingFacade;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkCreateResult;
import com.todak_todag.provider_service.provider.application.result.ProvideWorkUpdateResult;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Import(SecurityConfig.class)
@WebMvcTest(ProvideWorkApiController.class)
@DisplayName("제공 가능 일정 API")
class ProvideWorkApiControllerTest {

    private static final String BASE_URL = "/api/v1/service-offerings/{serviceOfferingId}/provide-works";

    private static final String UPDATE_URL =
            "/api/v1/service-offerings/{serviceOfferingId}/provide-works/{provideWorkId}";

    private static final String DELETE_URL =
            "/api/v1/service-offerings/{serviceOfferingId}/provide-works/{provideWorkId}";

    private final UUID serviceOfferingId = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvideWorkCommandService provideWorkCommandService;

    @MockitoBean
    private ServiceOfferingFacade serviceOfferingFacade;

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

    @Test
    @DisplayName("수정에 성공하면 200과 provideWorkId를 반환한다")
    void update_success() throws Exception {
        UUID provideWorkId = UUID.randomUUID();

        given(serviceOfferingFacade.updateProvideWork(any()))
                .willReturn(new ProvideWorkUpdateResult(provideWorkId));

        mockMvc.perform(patch(UPDATE_URL, serviceOfferingId, provideWorkId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2", "14:00", "18:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provideWorkId").value(provideWorkId.toString()));
    }

    @Test
    @DisplayName("수정 시 SERVICE_PROVIDER가 아니면 403")
    void update_forbidden() throws Exception {
        mockMvc.perform(patch(UPDATE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PATIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2", "14:00", "18:00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("존재하지 않는 제공 가능 일정이면 404")
    void update_notFound() throws Exception {
        given(serviceOfferingFacade.updateProvideWork(any()))
                .willThrow(new BusinessException(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND));

        mockMvc.perform(patch(UPDATE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2", "14:00", "18:00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROVIDE_WORK_NOT_FOUND"));
    }

    @Test
    @DisplayName("확정된 일정이 있으면 409")
    void update_scheduleExists() throws Exception {
        given(serviceOfferingFacade.updateProvideWork(any()))
                .willThrow(new BusinessException(ProviderErrorCode.PROVIDE_WORK_SCHEDULE_EXISTS));

        mockMvc.perform(patch(UPDATE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2", "14:00", "18:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROVIDE_WORK_SCHEDULE_EXISTS"));
    }

    @Test
    @DisplayName("수정 시 day가 범위를 벗어나면 400")
    void update_invalidDay() throws Exception {
        mockMvc.perform(patch(UPDATE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("0", "14:00", "18:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("삭제에 성공하면 204를 반환한다")
    void delete_success() throws Exception {
        mockMvc.perform(delete(DELETE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("삭제 시 SERVICE_PROVIDER가 아니면 403")
    void delete_forbidden() throws Exception {
        mockMvc.perform(delete(DELETE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("삭제 시 존재하지 않는 제공 가능 일정이면 404")
    void delete_notFound() throws Exception {
        doThrow(new BusinessException(ProviderErrorCode.PROVIDE_WORK_NOT_FOUND))
                .when(serviceOfferingFacade).deleteProvideWork(any());

        mockMvc.perform(delete(DELETE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROVIDE_WORK_NOT_FOUND"));
    }

    @Test
    @DisplayName("삭제 시 확정된 일정이 있으면 409")
    void delete_scheduleExists() throws Exception {
        doThrow(new BusinessException(ProviderErrorCode.PROVIDE_WORK_SCHEDULE_EXISTS))
                .when(serviceOfferingFacade).deleteProvideWork(any());

        mockMvc.perform(delete(DELETE_URL, serviceOfferingId, UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "SERVICE_PROVIDER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROVIDE_WORK_SCHEDULE_EXISTS"));
    }
}
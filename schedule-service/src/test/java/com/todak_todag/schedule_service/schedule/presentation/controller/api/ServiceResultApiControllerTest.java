package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceResultFacade;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceResultApiController.class)
@ActiveProfiles("test")
class ServiceResultApiControllerTest {

    private static final String REGISTER_URI = "/api/v1/service-results/%s";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceResultFacade serviceResultFacade;

    private String body(String startedAt, String finishedAt, String note) {
        return """
                { "startedAt": "%s", "finishedAt": "%s", "note": %s }
                """.formatted(startedAt, finishedAt, note == null ? "null" : "\"%s\"".formatted(note));
    }

    private String bodyWithoutNote(String startedAt, String finishedAt) {
        return """
                { "startedAt": "%s", "finishedAt": "%s" }
                """.formatted(startedAt, finishedAt);
    }

    @Nested
    @DisplayName("서비스 수행 결과 등록 API")
    class registerTest {

        @Test
        @DisplayName("status가 COMPLETED인 일정에 정상 등록되면 201과 serviceResultId를 반환한다")
        void register_completedSchedule_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceResultId = UUID.randomUUID();

            given(serviceResultFacade.register(any()))
                    .willReturn(new ServiceResultRegisterResult(serviceResultId));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", "정상적으로 서비스 제공 완료")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.serviceResultId").value(serviceResultId.toString()));
        }

        @Test
        @DisplayName("status가 NO_SHOW인 일정에도 정상 등록되면 201을 반환한다")
        void register_noShowSchedule_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceResultId = UUID.randomUUID();

            given(serviceResultFacade.register(any()))
                    .willReturn(new ServiceResultRegisterResult(serviceResultId));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", "예약 부도")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.serviceResultId").value(serviceResultId.toString()));
        }

        @Test
        @DisplayName("note 없이 요청해도 201을 반환한다 (note는 선택값)")
        void register_withoutNote_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID serviceResultId = UUID.randomUUID();

            given(serviceResultFacade.register(any()))
                    .willReturn(new ServiceResultRegisterResult(serviceResultId));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithoutNote("2026-09-01T09:00:00", "2026-09-01T10:00:00")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.serviceResultId").value(serviceResultId.toString()));
        }

        @Test
        @DisplayName("status가 COMPLETED/NO_SHOW가 아닌 일정(SCHEDULED 등)에 등록 시도하면 409를 반환한다")
        void register_invalidScheduleStatus_conflict() throws Exception {
            // given
            given(serviceResultFacade.register(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_INVALID_SCHEDULE_STATUS));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", null)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_INVALID_SCHEDULE_STATUS"));
        }

        @Test
        @DisplayName("이미 결과가 등록된 일정에 재등록을 시도하면 409를 반환한다")
        void register_alreadyExists_conflict() throws Exception {
            // given
            given(serviceResultFacade.register(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_RESULTS_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", null)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("본인이 배정된 서비스 제공자가 아니면 403을 반환한다")
        void register_notAssignedProvider_forbidden() throws Exception {
            // given
            given(serviceResultFacade.register(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("존재하지 않는 serviceScheduleId면 404를 반환한다 (07번 문서는 05번과 달리 404를 명시적으로 요구)")
        void register_notFound_notFound() throws Exception {
            // given
            given(serviceResultFacade.register(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND));

            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026-09-01T09:00:00", "2026-09-01T10:00:00", null)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_NOT_FOUND"));
        }

        @Test
        @DisplayName("startedAt이 없으면 400을 반환한다")
        void register_missingStartedAt_badRequest() throws Exception {
            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "finishedAt": "2026-09-01T10:00:00" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("finishedAt이 없으면 400을 반환한다")
        void register_missingFinishedAt_badRequest() throws Exception {
            // when & then
            mockMvc.perform(post(REGISTER_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "startedAt": "2026-09-01T09:00:00" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }
}

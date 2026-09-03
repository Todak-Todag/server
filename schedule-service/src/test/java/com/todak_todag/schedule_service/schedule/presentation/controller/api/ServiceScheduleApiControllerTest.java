package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceScheduleFacade;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceScheduleApiController.class)
class ServiceScheduleApiControllerTest {

    private static final String URI = "/api/v1/service-schedules/%s/status";
    private static final String CANCEL_URI = "/api/v1/service-schedules/%s/cancel";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceScheduleFacade serviceScheduleFacade;

    private String body(String date) {
        return """
                { "date": "%s" }
                """.formatted(date);
    }

    private String cancelBody(String cancelReason) {
        return """
                { "cancelReason": "%s" }
                """.formatted(cancelReason);
    }

    @Nested
    @DisplayName("서비스 일정 변경 API")
    class rescheduleTest {
        @Test
        @DisplayName("하루 앞당기기 요청이 유효하면 200과 RESCHEDULING 상태를 반환한다")
        void reschedule_dayBefore_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            LocalDate requestedDate = LocalDate.now().plusDays(2);

            given(serviceScheduleFacade.reschedule(any()))
                    .willReturn(new ServiceScheduleRescheduleResult(serviceScheduleId, ScheduleStatus.RESCHEDULING));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(requestedDate.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()))
                    .andExpect(jsonPath("$.data.status").value("RESCHEDULING"));
        }

        @Test
        @DisplayName("하루 미루기 요청이 유효하면 200과 RESCHEDULING 상태를 반환한다")
        void reschedule_dayAfter_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            LocalDate requestedDate = LocalDate.now().plusDays(4);

            given(serviceScheduleFacade.reschedule(any()))
                    .willReturn(new ServiceScheduleRescheduleResult(serviceScheduleId, ScheduleStatus.RESCHEDULING));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(requestedDate.toString())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("RESCHEDULING"));
        }

        @Test
        @DisplayName("당일 일정으로 앞당기려 하면 400을 반환한다")
        void reschedule_toToday_badRequest() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Care Plan 일정 범위를 초과하면 400을 반환한다")
        void reschedule_exceedsCarePlanRange_badRequest() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(4).toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE"));
        }

        @Test
        @DisplayName("status가 SCHEDULED가 아니면 400을 반환한다")
        void reschedule_invalidStatus_badRequest() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(2).toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING"));
        }

        @Test
        @DisplayName("일정 시작 24시간 이내 요청이면 400을 반환한다")
        void reschedule_withinDeadline_badRequest() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(1).toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED"));
        }

        @Test
        @DisplayName("본인 소유가 아닌 서비스 일정이면 403을 반환한다")
        void reschedule_notOwner_forbidden() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(2).toString())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("존재하지 않는 serviceScheduleId면 403을 반환한다 (리소스 존재 여부 비노출)")
        void reschedule_notFound_forbidden() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.reschedule(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(2).toString())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("date가 없으면 400을 반환한다")
        void reschedule_missingDate_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("date 형식이 올바르지 않으면 400을 반환한다")
        void reschedule_malformedDate_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("2026/09/01")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    @Nested
    @DisplayName("서비스 일정 취소 API")
    class cancelTest {
        @Test
        @DisplayName("취소 요청이 유효하면 200과 canceledAt을 반환한다")
        void cancel_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            LocalDateTime canceledAt = LocalDateTime.now();

            given(serviceScheduleFacade.cancel(any()))
                    .willReturn(new ServiceScheduleCancelResult(serviceScheduleId, canceledAt));

            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("개인 사정으로 취소합니다")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()));
        }

        @Test
        @DisplayName("이미 완료된 일정이면 409를 반환한다")
        void cancel_alreadyCompleted_conflict() throws Exception {
            // given
            given(serviceScheduleFacade.cancel(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL));

            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("취소 사유")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL"));
        }

        @Test
        @DisplayName("일정 시작 24시간 이내 취소 요청이면 409을 반환한다")
        void cancel_withinDeadline_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.cancel(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED));

            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("취소 사유")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED"));
        }

        @Test
        @DisplayName("본인 소유가 아닌 서비스 일정이면 403을 반환한다")
        void cancel_notOwner_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.cancel(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("취소 사유")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("존재하지 않는 serviceScheduleId면 403을 반환한다 (리소스 존재 여부 비노출)")
        void cancel_notFound_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.cancel(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("취소 사유")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("cancelReason이 없으면 400을 반환한다")
        void cancel_missingReason_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("cancelReason이 빈 문자열이면 400을 반환한다")
        void cancel_blankReason_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }
}

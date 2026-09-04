package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceScheduleFacade;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceScheduleApiController.class)
class ServiceScheduleApiControllerTest {

    private static final String SEARCH_URI = "/api/v1/service-schedules";
    private static final String DETAIL_URI = "/api/v1/service-schedules/%s";
    private static final String URI = "/api/v1/service-schedules/%s/status";
    private static final String CANCEL_URI = "/api/v1/service-schedules/%s/cancel";
    private static final String COMPLETE_URI = "/api/v1/service-schedules/%s/result";

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

    private String completeBody(String status) {
        return """
                { "status": "%s" }
                """.formatted(status);
    }

    @Nested
    @DisplayName("서비스 일정 상세 조회 API")
    class detailTest {

        private ServiceScheduleDetailResult sampleDetail(
                UUID serviceScheduleId,
                UUID servicePreferenceId,
                UUID serviceOfferingId,
                String cancelReason,
                LocalDateTime canceledAt
        ) {
            return new ServiceScheduleDetailResult(
                    serviceScheduleId,
                    servicePreferenceId,
                    serviceOfferingId,
                    cancelReason == null ? ScheduleStatus.SCHEDULED : ScheduleStatus.CANCELED,
                    LocalDate.of(2026, 9, 1),
                    LocalDateTime.of(2026, 9, 1, 9, 0),
                    LocalDateTime.of(2026, 9, 1, 10, 0),
                    cancelReason,
                    canceledAt
            );
        }

        @Test
        @DisplayName("퇴원 예정자가 본인 소유 일정을 조회하면 200과 상세 정보를 반환한다 (care-plan-service Internal API는 Facade에서 Mock 처리)")
        void detail_patient_owner_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID serviceOfferingId = UUID.randomUUID();
            ServiceScheduleDetailResult detail = sampleDetail(serviceScheduleId, servicePreferenceId, serviceOfferingId, null, null);

            given(serviceScheduleFacade.detail(any())).willReturn(detail);

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()))
                    .andExpect(jsonPath("$.data.servicePreferenceId").value(servicePreferenceId.toString()))
                    .andExpect(jsonPath("$.data.serviceOfferingId").value(serviceOfferingId.toString()))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        }

        @Test
        @DisplayName("서비스 제공자가 본인 담당 일정을 조회하면 200과 상세 정보를 반환한다 (provider-service Internal API는 Facade에서 Mock 처리)")
        void detail_serviceProvider_owner_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceScheduleDetailResult detail = sampleDetail(serviceScheduleId, UUID.randomUUID(), UUID.randomUUID(), null, null);

            given(serviceScheduleFacade.detail(any())).willReturn(detail);

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()));
        }

        @Test
        @DisplayName("존재하지 않는 serviceScheduleId면 403을 반환한다 (03/04/05번과 동일하게 리소스 존재 여부 비노출)")
        void detail_notFound_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("퇴원 예정자가 본인 소유가 아닌 일정을 조회하면 403을 반환한다 (patientId 불일치)")
        void detail_patient_notOwner_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("서비스 제공자가 본인 담당이 아닌 일정을 조회하면 403을 반환한다 (providerId 불일치)")
        void detail_serviceProvider_notOwner_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("취소된 일정을 조회하면 cancelReason/canceledAt이 값과 함께 반환된다")
        void detail_canceledSchedule_returnsCancelFields() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 31, 12, 0);
            ServiceScheduleDetailResult detail = sampleDetail(
                    serviceScheduleId, UUID.randomUUID(), UUID.randomUUID(), "개인 사정으로 취소합니다", canceledAt
            );

            given(serviceScheduleFacade.detail(any())).willReturn(detail);

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELED"))
                    .andExpect(jsonPath("$.data.cancelReason").value("개인 사정으로 취소합니다"))
                    .andExpect(jsonPath("$.data.canceledAt").value("2026-08-31T12:00:00"));
        }

        @Test
        @DisplayName("취소되지 않은 일정을 조회하면 cancelReason/canceledAt이 null로 반환된다")
        void detail_notCanceledSchedule_returnsNullCancelFields() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            ServiceScheduleDetailResult detail = sampleDetail(serviceScheduleId, UUID.randomUUID(), UUID.randomUUID(), null, null);

            given(serviceScheduleFacade.detail(any())).willReturn(detail);

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cancelReason").doesNotExist())
                    .andExpect(jsonPath("$.data.canceledAt").doesNotExist());
        }

        @Test
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 반환한다")
        void detail_unsupportedRole_forbidden() throws Exception {
            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SOCIAL_WORKER"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("서비스 일정 목록 조회 API")
    class searchTest {

        private ServiceScheduleSearchResult sampleResult() {
            return new ServiceScheduleSearchResult(
                    UUID.randomUUID(),
                    ScheduleStatus.SCHEDULED,
                    LocalDate.of(2026, 9, 1),
                    LocalDateTime.of(2026, 9, 1, 9, 0),
                    LocalDateTime.of(2026, 9, 1, 10, 0)
            );
        }

        @Test
        @DisplayName("퇴원 예정자 역할로 조회하면 200과 목록을 반환한다 (care-plan-service Internal API는 Facade에서 Mock 처리)")
        void search_patient_success() throws Exception {
            // given
            ServiceScheduleSearchResult result = sampleResult();
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].serviceScheduleId").value(result.serviceScheduleId().toString()))
                    .andExpect(jsonPath("$.data.content[0].status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"));
        }

        @Test
        @DisplayName("서비스 제공자 역할로 조회하면 200과 목록을 반환한다 (provider-service Internal API는 Facade에서 Mock 처리)")
        void search_serviceProvider_success() throws Exception {
            // given
            ServiceScheduleSearchResult result = sampleResult();
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].serviceScheduleId").value(result.serviceScheduleId().toString()));
        }

        @Test
        @DisplayName("status 필터를 전달하면 파싱된 ScheduleStatus로 Facade를 호출한다")
        void search_statusFilter_passedToFacade() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("status", "SCHEDULED"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(ScheduleStatus.SCHEDULED);
        }

        @Test
        @DisplayName("date 필터를 전달하면 파싱된 LocalDate로 Facade를 호출한다")
        void search_dateFilter_passedToFacade() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("date", "2026-09-01"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            assertThat(captor.getValue().date()).isEqualTo(LocalDate.of(2026, 9, 1));
        }

        @Test
        @DisplayName("허용되지 않는 status 값이면 400과 SERVICE_SCHEDULE_INVALID_STATUS_FILTER를 반환한다")
        void search_invalidStatus_badRequest() throws Exception {
            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_INVALID_STATUS_FILTER"));
        }

        @Test
        @DisplayName("size에 10/30/50 외의 값을 요청하면 10으로 자동 보정된다")
        void search_invalidSize_correctedToTen() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("size", "20"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 배열과 올바른 pageInfo를 반환한다")
        void search_empty_returnsEmptyContentAndPageInfo() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages").value(0));
        }

        @Test
        @DisplayName("page/size가 정상적으로 Pageable에 반영된다")
        void search_pagination_reflectedInPageable() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("page", "2")
                            .param("size", "30"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            Pageable pageable = captor.getValue().pageable();
            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(30);
        }

        @Test
        @DisplayName("sort=createdAt,ASC를 요청하면 오래된순으로 정렬 조건이 반영된다")
        void search_sortAscending_reflectedInPageable() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("sort", "createdAt,ASC"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getSort().getOrderFor("createdAt").isAscending()).isTrue();
        }

        @Test
        @DisplayName("sort 파라미터가 없으면 기본값(createdAt,DESC)이 적용된다")
        void search_defaultSort_isCreatedAtDesc() throws Exception {
            // given
            given(serviceScheduleFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceScheduleSearchQuery> captor = ArgumentCaptor.forClass(ServiceScheduleSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            verify(serviceScheduleFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        }

        @Test
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 반환한다")
        void search_unsupportedRole_forbidden() throws Exception {
            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SOCIAL_WORKER"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }
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

    @Nested
    @DisplayName("서비스 수행 완료 API")
    class completeTest {
        @Test
        @DisplayName("정상 완료(COMPLETED) 요청이면 200과 COMPLETED 상태를 반환한다")
        void complete_completed_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.complete(any()))
                    .willReturn(new ServiceScheduleCompleteResult(serviceScheduleId, ScheduleStatus.COMPLETED));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("정상 미완료(NO_SHOW) 요청이면 200과 NO_SHOW 상태를 반환한다")
        void complete_noShow_success() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();

            given(serviceScheduleFacade.complete(any()))
                    .willReturn(new ServiceScheduleCompleteResult(serviceScheduleId, ScheduleStatus.NO_SHOW));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(serviceScheduleId))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("NO_SHOW")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("NO_SHOW"));
        }

        @Test
        @DisplayName("status가 SCHEDULED가 아닌 일정이면 409를 반환한다")
        void complete_invalidStatus_conflict() throws Exception {
            // given
            given(serviceScheduleFacade.complete(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED"));
        }

        @Test
        @DisplayName("본인이 배정된 서비스 제공자가 아니면 403을 반환한다")
        void complete_notAssignedProvider_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.complete(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("존재하지 않는 serviceScheduleId면 403을 반환한다 (리소스 존재 여부 비노출)")
        void complete_notFound_forbidden() throws Exception {
            // given
            given(serviceScheduleFacade.complete(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("finishedAt 이전 요청이면 400을 반환한다")
        void complete_beforeFinishedAt_badRequest() throws Exception {
            // given
            given(serviceScheduleFacade.complete(any()))
                    .willThrow(new BusinessException(ScheduleErrorCode.SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY));

            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY"));
        }

        @Test
        @DisplayName("status가 없으면 400을 반환한다")
        void complete_missingStatus_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }

        @Test
        @DisplayName("status가 COMPLETED/NO_SHOW가 아니면 400을 반환한다")
        void complete_invalidStatusValue_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("CANCELED")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }
}

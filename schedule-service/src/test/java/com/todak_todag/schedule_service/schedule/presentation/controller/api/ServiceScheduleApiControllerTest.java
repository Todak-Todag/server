package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.common.UserRole;
import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.FeignErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceScheduleFacade;
import com.todak_todag.schedule_service.schedule.application.query.ServiceScheduleSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleDetailResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import feign.codec.DecodeException;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.todak_todag.schedule_service.support.AuthenticatedRequestSupport.asUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceScheduleApiController.class)
@ActiveProfiles("test")
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cancelReason").doesNotExist())
                    .andExpect(jsonPath("$.data.canceledAt").doesNotExist());
        }

        @Test
        @DisplayName("퇴원 예정자/서비스 제공자가 아닌 역할이면 403을 반환한다")
        void detail_unsupportedRole_forbidden() throws Exception {
            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.SOCIAL_WORKER)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SOCIAL_WORKER)))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
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
                            .with(asUser(UUID.randomUUID(), UserRole.SERVICE_PROVIDER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("CANCELED")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        }
    }

    @Nested
    @DisplayName("인증")
    class authenticationTest {

        @Test
        @DisplayName("인증 정보가 없으면 서비스 일정 목록 조회는 401을 반환한다")
        void search_withoutAuthentication_unauthorized() throws Exception {
            // given & when & then
            mockMvc.perform(get(SEARCH_URI))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(serviceScheduleFacade);
        }

        @Test
        @DisplayName("인증 정보가 없으면 서비스 일정 상세 조회는 401을 반환한다")
        void detail_withoutAuthentication_unauthorized() throws Exception {
            // given & when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(serviceScheduleFacade);
        }

        @Test
        @DisplayName("인증 정보가 없으면 서비스 일정 변경은 401을 반환한다")
        void reschedule_withoutAuthentication_unauthorized() throws Exception {
            // given & when & then
            mockMvc.perform(patch(URI.formatted(UUID.randomUUID()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().plusDays(3).toString())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(serviceScheduleFacade);
        }

        @Test
        @DisplayName("인증 정보가 없으면 서비스 일정 취소는 401을 반환한다")
        void cancel_withoutAuthentication_unauthorized() throws Exception {
            // given & when & then
            mockMvc.perform(patch(CANCEL_URI.formatted(UUID.randomUUID()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody("개인 사정으로 취소합니다")))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(serviceScheduleFacade);
        }

        @Test
        @DisplayName("인증 정보가 없으면 서비스 수행 완료는 401을 반환한다")
        void complete_withoutAuthentication_unauthorized() throws Exception {
            // given & when & then
            mockMvc.perform(patch(COMPLETE_URI.formatted(UUID.randomUUID()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(completeBody("COMPLETED")))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(serviceScheduleFacade);
        }
    }

    // Internal API(Feign) 호출 실패가 실제 HTTP 응답으로 어떻게 나가는지 검증한다.
    //
    // 이전에는 아래 예외가 전부 GlobalExceptionHandler의 Exception 핸들러로 떨어져
    // 500 INTERNAL_SERVER_ERROR 하나로 응답했다.
    // 예외 타입/ErrorCode는 InternalApiErrorDecoderTest가 실제 Feign 호출로 검증하고,
    // 여기서는 그 예외가 status/code/body로 이어지는 마지막 구간을 본다.
    @Nested
    @DisplayName("Internal API 호출 실패")
    class internalApiFailureTest {

        private Request feignRequest() {
            return Request.create(
                    Request.HttpMethod.GET, "/internal/v1/service-preferences/x/care-plan",
                    Map.of(), null, StandardCharsets.UTF_8
            );
        }

        @Test
        @DisplayName("상대 서비스가 404를 반환하면 403 AUTH_FORBIDDEN을 반환한다")
        void detail_downstreamNotFound_forbidden() throws Exception {
            // given — ErrorDecoder가 404를 AUTH_FORBIDDEN으로 바꿔 던진 상황
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(CommonErrorCode.AUTH_FORBIDDEN));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        }

        @Test
        @DisplayName("상대 서비스가 4xx로 요청을 거부하면 500 EXTERNAL_SERVICE_CALL_REJECTED를 반환한다")
        void detail_downstreamRejected_internalServerErrorWithOwnCode() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED));

            // when & then — status는 기존과 같은 500이지만 error code로 원인이 구분된다
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_CALL_REJECTED"))
                    .andExpect(jsonPath("$.details.reason").value(
                            FeignErrorCode.EXTERNAL_SERVICE_CALL_REJECTED.getMessage()))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("상대 서비스가 5xx를 반환하면 503 EXTERNAL_SERVICE_UNAVAILABLE을 반환한다")
        void detail_downstreamServerError_serviceUnavailable() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new BusinessException(FeignErrorCode.EXTERNAL_SERVICE_UNAVAILABLE));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
        }

        @Test
        @DisplayName("상대 서비스에 연결하지 못하면 503 EXTERNAL_SERVICE_UNAVAILABLE을 반환한다")
        void detail_connectionFailure_serviceUnavailable() throws Exception {
            // given — HTTP 응답이 없어 ErrorDecoder를 타지 못하고 RetryableException이 그대로 올라온다
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new RetryableException(
                            -1, "Connection refused", Request.HttpMethod.GET, (Long) null, feignRequest()
                    ));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"));
        }

        @Test
        @DisplayName("상대 서비스 응답을 디코딩하지 못하면 502 EXTERNAL_SERVICE_RESPONSE_INVALID를 반환한다")
        void detail_decodeFailure_badGateway() throws Exception {
            // given — 응답(2xx)은 받았으나 역직렬화에 실패한 경우
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new DecodeException(200, "cannot decode", feignRequest()));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_RESPONSE_INVALID"));
        }

        @Test
        @DisplayName("Feign 예외 응답에는 상대 서비스의 오류 본문이 실리지 않는다")
        void detail_feignFailure_doesNotLeakDownstreamMessage() throws Exception {
            // given
            given(serviceScheduleFacade.detail(any()))
                    .willThrow(new FeignException.InternalServerError(
                            "relation \"care_plan_schema.p_care_plans\" does not exist",
                            feignRequest(), null, Map.of()
                    ));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(UUID.randomUUID()))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.message").value(
                            FeignErrorCode.EXTERNAL_SERVICE_RESPONSE_INVALID.getMessage()));
        }

        @Test
        @DisplayName("Internal API 호출이 정상이면 기존과 동일하게 200을 반환한다")
        void detail_success_unchanged() throws Exception {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            given(serviceScheduleFacade.detail(any())).willReturn(new ServiceScheduleDetailResult(
                    serviceScheduleId, UUID.randomUUID(), UUID.randomUUID(), ScheduleStatus.SCHEDULED,
                    LocalDate.of(2026, 9, 1),
                    LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 1, 10, 0),
                    null, null
            ));

            // when & then
            mockMvc.perform(get(DETAIL_URI.formatted(serviceScheduleId))
                            .with(asUser(UUID.randomUUID(), UserRole.PATIENT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()));
        }
    }
}

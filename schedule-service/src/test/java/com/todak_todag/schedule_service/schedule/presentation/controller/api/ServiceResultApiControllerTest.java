package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceResultFacade;
import com.todak_todag.schedule_service.schedule.application.query.ServiceResultSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ServiceResultApiController.class)
@ActiveProfiles("test")
class ServiceResultApiControllerTest {

    private static final String REGISTER_URI = "/api/v1/service-results/%s";
    private static final String SEARCH_URI = "/api/v1/service-results";

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

    @Nested
    @DisplayName("서비스 수행 결과 목록 조회 API")
    class searchTest {

        private ServiceResultSearchResult sampleResult() {
            return new ServiceResultSearchResult(
                    UUID.randomUUID(),
                    LocalDateTime.of(2026, 9, 1, 9, 0),
                    LocalDateTime.of(2026, 9, 1, 10, 0)
            );
        }

        @Test
        @DisplayName("퇴원 예정자 역할로 조회하면 200과 목록을 반환한다 (care-plan-service Internal API는 Facade에서 Mock 처리)")
        void search_patient_success() throws Exception {
            // given
            ServiceResultSearchResult result = sampleResult();
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("서비스 수행 결과 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].serviceResultId").value(result.serviceResultId().toString()))
                    .andExpect(jsonPath("$.data.content[0].startedAt").value("2026-09-01T09:00:00"))
                    .andExpect(jsonPath("$.data.content[0].finishedAt").value("2026-09-01T10:00:00"))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size").value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1));
        }

        @Test
        @DisplayName("퇴원 예정자 역할이면 UserContext의 userId/role이 그대로 Facade에 전달된다 (소유권 필터링 기준)")
        void search_patient_passesUserContextToFacade() throws Exception {
            // given
            UUID patientId = UUID.randomUUID();
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", patientId.toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(patientId);
            assertThat(captor.getValue().role().name()).isEqualTo("PATIENT");
        }

        @Test
        @DisplayName("서비스 제공자 역할로 조회하면 200과 목록을 반환한다 (provider-service Internal API는 Facade에서 Mock 처리)")
        void search_serviceProvider_success() throws Exception {
            // given
            ServiceResultSearchResult result = sampleResult();
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].serviceResultId").value(result.serviceResultId().toString()))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"));
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 배열과 올바른 pageInfo를 반환한다")
        void search_noResult_returnsEmptyArrayWithPageInfo() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size").value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages").value(0));
        }

        @Test
        @DisplayName("page/size를 전달하면 그대로 Pageable에 반영된다")
        void search_pagination_passedToFacade() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("page", "2")
                            .param("size", "30"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(30);
        }

        @Test
        @DisplayName("page/size를 생략하면 기본값 0/10이 적용된다")
        void search_defaultPagination_appliesDefaults() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getPageNumber()).isZero();
            assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("size에 10/30/50 외의 값(20)을 요청하면 10으로 자동 보정된다")
        void search_invalidSize_correctedToTen() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("size", "20"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("sort를 생략하면 기본값 createdAt,DESC(최신순)가 적용된다")
        void search_defaultSort_isCreatedAtDesc() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            Sort.Order order = captor.getValue().pageable().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.isDescending()).isTrue();
        }

        @Test
        @DisplayName("sort에 createdAt,ASC(오래된순)를 전달하면 오름차순으로 파싱된다")
        void search_sortAscending_parsedAsAsc() throws Exception {
            // given
            given(serviceResultFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of()));
            ArgumentCaptor<ServiceResultSearchQuery> captor = ArgumentCaptor.forClass(ServiceResultSearchQuery.class);

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT")
                            .param("sort", "createdAt,ASC"))
                    .andExpect(status().isOk());

            // then
            verify(serviceResultFacade).search(captor.capture());
            Sort.Order order = captor.getValue().pageable().getSort().getOrderFor("createdAt");
            assertThat(order).isNotNull();
            assertThat(order.isAscending()).isTrue();
        }
    }
}

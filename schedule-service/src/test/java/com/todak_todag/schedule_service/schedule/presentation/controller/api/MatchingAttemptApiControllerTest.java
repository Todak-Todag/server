package com.todak_todag.schedule_service.schedule.presentation.controller.api;

import com.todak_todag.schedule_service.global.config.SecurityConfig;
import com.todak_todag.schedule_service.schedule.application.facade.ServiceMatchingAttemptFacade;
import com.todak_todag.schedule_service.schedule.application.query.MatchingAttemptSearchQuery;
import com.todak_todag.schedule_service.schedule.application.result.MatchingAttemptSearchResult;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(MatchingAttemptApiController.class)
@ActiveProfiles("test")
class MatchingAttemptApiControllerTest {

    private static final String SEARCH_URI = "/api/v1/matching-attempts";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceMatchingAttemptFacade serviceMatchingAttemptFacade;

    private MatchingAttemptSearchResult failedResult(
            UUID matchingAttemptId,
            UUID servicePreferenceId,
            UUID provideServiceId
    ) {
        return new MatchingAttemptSearchResult(
                matchingAttemptId,
                servicePreferenceId,
                provideServiceId,
                LocalDate.of(2026, 9, 1),
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.FAILED,
                "해당 날짜/시간대에 제공 가능한 서비스 제공자 없음",
                Instant.parse("2026-09-01T09:00:00Z"),
                null
        );
    }

    private MatchingAttemptSearchQuery captureQuery() {
        ArgumentCaptor<MatchingAttemptSearchQuery> captor =
                ArgumentCaptor.forClass(MatchingAttemptSearchQuery.class);
        verify(serviceMatchingAttemptFacade).search(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("status 필터")
    class statusFilterTest {

        @Test
        @DisplayName("status 파라미터가 없으면 기본값 FAILED로 조회한다")
        void search_withoutStatus_defaultsToFailed() throws Exception {
            // given
            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // then
            assertThat(captureQuery().status()).isEqualTo(MatchingAttemptStatus.FAILED);
        }

        @Test
        @DisplayName("status=MATCHED로 요청하면 MATCHED 필터로 조회한다")
        void search_withMatchedStatus() throws Exception {
            // given
            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .param("status", "MATCHED")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            MatchingAttemptSearchQuery query = captureQuery();
            assertThat(query.status()).isEqualTo(MatchingAttemptStatus.MATCHED);
            assertThat(query.excludeAlreadyScheduled()).isFalse();
        }

        @Test
        @DisplayName("status에 허용되지 않는 값이 들어오면 400을 반환한다")
        void search_withInvalidStatus_badRequest() throws Exception {
            // given & when & then
            mockMvc.perform(get(SEARCH_URI)
                            .param("status", "WRONG")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

            verify(serviceMatchingAttemptFacade, never()).search(any());
        }
    }

    @Nested
    @DisplayName("페이지네이션")
    class paginationTest {

        @Test
        @DisplayName("size가 10/30/50이 아니면 10으로 자동 변경되어 조회된다")
        void search_withDisallowedSize_fallsBackToTen() throws Exception {
            // given
            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .param("size", "7")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            Pageable pageable = captureQuery().pageable();
            assertThat(pageable.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("size가 30이면 그대로 사용된다")
        void search_withAllowedSize() throws Exception {
            // given
            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 30), 0));

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .param("size", "30")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            assertThat(captureQuery().pageable().getPageSize()).isEqualTo(30);
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 배열과 pageInfo를 반환한다")
        void search_emptyResult() throws Exception {
            // given
            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Response 스펙")
    class responseTest {

        @Test
        @DisplayName("15번 문서 Response 표의 필드/타입/옵션과 정확히 일치한다")
        void search_responsePayloadMatchesSpec() throws Exception {
            // given
            UUID matchingAttemptId = UUID.randomUUID();
            UUID servicePreferenceId = UUID.randomUUID();
            UUID provideServiceId = UUID.randomUUID();

            given(serviceMatchingAttemptFacade.search(any())).willReturn(
                    new PageImpl<>(
                            List.of(failedResult(matchingAttemptId, servicePreferenceId, provideServiceId)),
                            PageRequest.of(0, 10),
                            1
                    )
            );

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("매칭 실패 내역 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].matchingAttemptId").value(matchingAttemptId.toString()))
                    .andExpect(jsonPath("$.data.content[0].servicePreferenceId").value(servicePreferenceId.toString()))
                    .andExpect(jsonPath("$.data.content[0].provideServiceId").value(provideServiceId.toString()))
                    .andExpect(jsonPath("$.data.content[0].date").value("2026-09-01"))
                    .andExpect(jsonPath("$.data.content[0].preferredTimeSlot").value("MORNING"))
                    .andExpect(jsonPath("$.data.content[0].status").value("FAILED"))
                    .andExpect(jsonPath("$.data.content[0].failureReason")
                            .value("해당 날짜/시간대에 제공 가능한 서비스 제공자 없음"))
                    .andExpect(jsonPath("$.data.content[0].failedAt").value("2026-09-01T09:00:00Z"))
                    .andExpect(jsonPath("$.data.content[0].matchedAt").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.pageInfo.paginationType").value("OFFSET"))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(0))
                    .andExpect(jsonPath("$.data.pageInfo.size").value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1));
        }

        @Test
        @DisplayName("preferredTimeSlot이 없는 시도는 null로 반환한다")
        void search_nullPreferredTimeSlot() throws Exception {
            // given
            MatchingAttemptSearchResult result = new MatchingAttemptSearchResult(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 1),
                    null,
                    MatchingAttemptStatus.MATCHED,
                    null,
                    null,
                    Instant.parse("2026-08-29T10:00:00Z")
            );

            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

            // when & then
            mockMvc.perform(get(SEARCH_URI)
                            .param("status", "MATCHED")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].preferredTimeSlot").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.content[0].failureReason").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.content[0].failedAt").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.content[0].matchedAt").value("2026-08-29T10:00:00Z"));
        }
    }

    @Nested
    @DisplayName("인가")
    class authorizationTest {

        @Test
        @DisplayName("퇴원 예정자가 아니면 403을 반환한다")
        void search_nonPatient_forbidden() throws Exception {
            // given & when & then
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "SERVICE_PROVIDER"))
                    .andExpect(status().isForbidden());

            verify(serviceMatchingAttemptFacade, never()).search(any());
        }

        @Test
        @DisplayName("요청자가 담당하는 servicePreferenceId 목록으로만 조회되도록 userId를 전달한다")
        void search_passesRequesterUserId() throws Exception {
            // given
            UUID userId = UUID.randomUUID();

            given(serviceMatchingAttemptFacade.search(any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            // when
            mockMvc.perform(get(SEARCH_URI)
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "PATIENT"))
                    .andExpect(status().isOk());

            // then
            assertThat(captureQuery().userId()).isEqualTo(userId);
        }
    }
}

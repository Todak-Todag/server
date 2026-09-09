package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.result.MatchingResultQueryResult;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingQueryService;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SocialWorkerMatchingQueryApiControllerTest {

    private MatchingQueryService matchingQueryService;
    private SocialWorkerMatchingQueryApiController controller;

    @BeforeEach
    void setUp() {

        matchingQueryService =
                mock(MatchingQueryService.class);

        controller =
                new SocialWorkerMatchingQueryApiController(
                        matchingQueryService
                );
    }

    @Test
    @DisplayName("PATIENT는 자신의 사회복지사 매칭 결과를 조회할 수 있다")
    void patientCanGetMatchingResult() {

        UUID matchingResultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID socialWorkerId = UUID.randomUUID();

        Instant requestedAt =
                Instant.parse(
                        "2026-09-09T01:00:00Z"
                );

        Instant assignedAt =
                Instant.parse(
                        "2026-09-09T01:01:00Z"
                );

        UserContext userContext =
                UserContext.from(
                        patientId.toString(),
                        "PATIENT"
                );

        MatchingResultQueryResult result =
                new MatchingResultQueryResult(
                        matchingResultId,
                        patientId,
                        socialWorkerId,
                        MatchingStatus.ACTIVE,
                        requestedAt,
                        assignedAt
                );

        when(
                matchingQueryService
                        .getLatestResult(
                                patientId
                        )
        ).thenReturn(
                result
        );

        ResponseEntity<ApiResponse<MatchingResultResponse>> response =
                controller.getMatchingResult(
                        userContext
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertTrue(
                response.getBody()
                        .success()
        );

        assertEquals(
                200,
                response.getBody()
                        .code()
        );

        assertEquals(
                "사회복지사 매칭 결과 조회 성공",
                response.getBody()
                        .message()
        );

        MatchingResultResponse data =
                response.getBody()
                        .data();

        assertEquals(
                matchingResultId,
                data.matchingResultId()
        );

        assertEquals(
                patientId,
                data.patientId()
        );

        assertEquals(
                socialWorkerId,
                data.socialWorkerId()
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                data.status()
        );

        assertEquals(
                requestedAt,
                data.requestedAt()
        );

        assertEquals(
                assignedAt,
                data.assignedAt()
        );

        verify(
                matchingQueryService,
                times(1)
        ).getLatestResult(
                patientId
        );
    }

    @Test
    @DisplayName("PATIENT가 아닌 사용자는 매칭 결과를 조회할 수 없다")
    void nonPatientCannotGetMatchingResult() {

        UUID userId =
                UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "ADMIN"
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller
                                        .getMatchingResult(
                                                userContext
                                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingQueryService
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 매칭 결과 조회를 거부한다")
    void nullUserContextIsForbidden() {

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller
                                        .getMatchingResult(
                                                null
                                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingQueryService
        );
    }
}
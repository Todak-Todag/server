package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.query.MatchingResultQuery;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    @DisplayName("인증된 사용자는 matchingResultId로 매칭 결과를 조회한다")
    void authenticatedUserCanGetMatchingResult() {

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
                        .getResult(
                                any(MatchingResultQuery.class)
                        )
        ).thenReturn(
                result
        );

        ResponseEntity<ApiResponse<MatchingResultResponse>> response =
                controller.getMatchingResult(
                        matchingResultId,
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
                matchingQueryService
        ).getResult(
                new MatchingResultQuery(
                        matchingResultId,
                        patientId,
                        UserRole.PATIENT
                )
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 매칭 결과 조회를 거부한다")
    void nullUserContextIsForbidden() {

        UUID matchingResultId =
                UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller
                                        .getMatchingResult(
                                                matchingResultId,
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
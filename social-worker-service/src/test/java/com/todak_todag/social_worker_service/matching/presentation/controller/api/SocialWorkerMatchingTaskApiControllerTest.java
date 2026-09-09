package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.result.MatchingTaskStatusResult;
import com.todak_todag.social_worker_service.matching.application.service.query.MatchingTaskQueryService;
import com.todak_todag.social_worker_service.matching.application.support.task.MatchingTaskStatus;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingTaskStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialWorkerMatchingTaskApiControllerTest {

    private MatchingTaskQueryService matchingTaskQueryService;
    private SocialWorkerMatchingTaskApiController controller;

    @BeforeEach
    void setUp() {

        matchingTaskQueryService =
                mock(MatchingTaskQueryService.class);

        controller =
                new SocialWorkerMatchingTaskApiController(
                        matchingTaskQueryService
                );
    }

    @Test
    @DisplayName("PATIENT가 본인의 매칭 처리 현황을 조회하면 200 OK를 반환한다")
    void patientCanGetMatchingTaskStatus() {

        UUID patientId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID matchingResultId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        patientId.toString(),
                        "PATIENT"
                );

        MatchingTaskStatusResult result =
                new MatchingTaskStatusResult(
                        taskId,
                        MatchingTaskStatus.COMPLETED,
                        matchingResultId
                );

        when(
                matchingTaskQueryService
                        .getStatus(
                                taskId,
                                patientId
                        )
        ).thenReturn(result);

        ResponseEntity<ApiResponse<MatchingTaskStatusResponse>> response =
                controller.getMatchingTaskStatus(
                        taskId,
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
                response.getBody().success()
        );

        assertEquals(
                200,
                response.getBody().code()
        );

        assertEquals(
                "사회복지사 매칭 처리 현황 조회 성공",
                response.getBody().message()
        );

        assertEquals(
                taskId,
                response.getBody()
                        .data()
                        .taskId()
        );

        assertEquals(
                MatchingTaskStatus.COMPLETED,
                response.getBody()
                        .data()
                        .taskStatus()
        );

        assertEquals(
                matchingResultId,
                response.getBody()
                        .data()
                        .matchingResultId()
        );

        verify(
                matchingTaskQueryService,
                times(1)
        ).getStatus(
                taskId,
                patientId
        );
    }

    @Test
    @DisplayName("FAILED Task 조회 시 matchingResultId는 null로 반환한다")
    void failedTaskReturnsNullMatchingResultId() {

        UUID patientId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        patientId.toString(),
                        "PATIENT"
                );

        MatchingTaskStatusResult result =
                new MatchingTaskStatusResult(
                        taskId,
                        MatchingTaskStatus.FAILED,
                        null
                );

        when(
                matchingTaskQueryService
                        .getStatus(
                                taskId,
                                patientId
                        )
        ).thenReturn(result);

        ResponseEntity<ApiResponse<MatchingTaskStatusResponse>> response =
                controller.getMatchingTaskStatus(
                        taskId,
                        userContext
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                MatchingTaskStatus.FAILED,
                response.getBody()
                        .data()
                        .taskStatus()
        );

        assertNull(
                response.getBody()
                        .data()
                        .matchingResultId()
        );
    }

    @Test
    @DisplayName("PATIENT가 아닌 사용자는 매칭 처리 현황을 조회할 수 없다")
    void nonPatientCannotGetMatchingTaskStatus() {

        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "ADMIN"
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller.getMatchingTaskStatus(
                                        taskId,
                                        userContext
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_TASK_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingTaskQueryService
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 매칭 처리 현황 조회를 거부한다")
    void nullUserContextIsForbidden() {

        UUID taskId = UUID.randomUUID();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller.getMatchingTaskStatus(
                                        taskId,
                                        null
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_TASK_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingTaskQueryService
        );
    }
}
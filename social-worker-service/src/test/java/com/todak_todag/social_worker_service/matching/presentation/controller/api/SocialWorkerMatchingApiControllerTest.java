package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.response.ApiResponse;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingRequestCommandService;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.response.MatchingRequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocialWorkerMatchingApiControllerTest {

    private MatchingRequestCommandService matchingRequestCommandService;
    private SocialWorkerMatchingApiController controller;

    @BeforeEach
    void setUp() {

        matchingRequestCommandService =
                mock(MatchingRequestCommandService.class);

        controller =
                new SocialWorkerMatchingApiController(
                        matchingRequestCommandService
                );
    }

    @Test
    @DisplayName("PATIENT가 매칭을 요청하면 202 Accepted와 taskId를 반환한다")
    void patientCanRequestMatching() {

        UUID patientId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        patientId.toString(),
                        "PATIENT"
                );

        when(
                matchingRequestCommandService
                        .request(patientId)
        ).thenReturn(taskId);

        ResponseEntity<ApiResponse<MatchingRequestResponse>> response =
                controller.requestMatching(
                        userContext
                );

        assertEquals(
                HttpStatus.ACCEPTED,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertTrue(
                response.getBody().success()
        );

        assertEquals(
                202,
                response.getBody().code()
        );

        assertEquals(
                "사회복지사 매칭 요청 접수 성공",
                response.getBody().message()
        );

        assertEquals(
                taskId,
                response.getBody()
                        .data()
                        .taskId()
        );

        verify(
                matchingRequestCommandService,
                times(1)
        ).request(patientId);
    }

    @Test
    @DisplayName("PATIENT가 아닌 사용자는 사회복지사 매칭을 요청할 수 없다")
    void nonPatientCannotRequestMatching() {

        UUID userId = UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        userId.toString(),
                        "ADMIN"
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller.requestMatching(
                                        userContext
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingRequestCommandService
        );
    }

    @Test
    @DisplayName("사용자 인증 정보가 없으면 매칭 요청을 거부한다")
    void nullUserContextIsForbidden() {

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller.requestMatching(
                                        null
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingRequestCommandService
        );
    }
}
package com.todak_todag.social_worker_service.matching.presentation.controller.api;

import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.global.security.UserContext;
import com.todak_todag.social_worker_service.matching.application.command.MatchingStatusChangeCommand;
import com.todak_todag.social_worker_service.matching.application.result.MatchingStatusChangeResult;
import com.todak_todag.social_worker_service.matching.application.service.command.MatchingStatusCommandService;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import com.todak_todag.social_worker_service.matching.presentation.request.MatchingStatusChangeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SocialWorkerMatchingStatusApiControllerTest {

    private MatchingStatusCommandService matchingStatusCommandService;
    private SocialWorkerMatchingStatusApiController controller;

    @BeforeEach
    void setUp() {

        matchingStatusCommandService =
                mock(
                        MatchingStatusCommandService.class
                );

        controller =
                new SocialWorkerMatchingStatusApiController(
                        matchingStatusCommandService
                );
    }

    @Test
    @DisplayName("사회복지사가 매칭 종료를 요청하면 200과 변경 결과를 반환한다")
    void socialWorkerCanRequestStatusChange() {

        UUID matchingResultId =
                UUID.randomUUID();

        UUID socialWorkerId =
                UUID.randomUUID();

        UserContext userContext =
                UserContext.from(
                        socialWorkerId.toString(),
                        "SOCIAL_WORKER"
                );

        when(
                matchingStatusCommandService
                        .changeStatus(
                                any(
                                        MatchingStatusChangeCommand.class
                                )
                        )
        ).thenReturn(
                new MatchingStatusChangeResult(
                        matchingResultId,
                        MatchingStatus.ENDED
                )
        );

        var response =
                controller.changeStatus(
                        matchingResultId,
                        new MatchingStatusChangeRequest(
                                "ENDED"
                        ),
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
                200,
                response.getBody().code()
        );

        assertEquals(
                "사회복지사 매칭 상태 변경 성공",
                response.getBody().message()
        );

        assertEquals(
                matchingResultId,
                response.getBody()
                        .data()
                        .matchingResultId()
        );

        assertEquals(
                MatchingStatus.ENDED,
                response.getBody()
                        .data()
                        .status()
        );

        ArgumentCaptor<MatchingStatusChangeCommand> captor =
                ArgumentCaptor.forClass(
                        MatchingStatusChangeCommand.class
                );

        verify(
                matchingStatusCommandService
        ).changeStatus(
                captor.capture()
        );

        assertEquals(
                matchingResultId,
                captor.getValue()
                        .matchingResultId()
        );

        assertEquals(
                socialWorkerId,
                captor.getValue()
                        .requesterId()
        );
    }

    @Test
    @DisplayName("인증 정보가 없으면 매칭 상태 변경을 거부한다")
    void anonymousUserIsForbidden() {

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                controller.changeStatus(
                                        UUID.randomUUID(),
                                        new MatchingStatusChangeRequest(
                                                "ENDED"
                                        ),
                                        null
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_STATUS_CHANGE_FORBIDDEN,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingStatusCommandService
        );
    }
}
package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.command.MatchingStatusChangeCommand;
import com.todak_todag.social_worker_service.matching.application.result.MatchingStatusChangeResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MatchingStatusCommandServiceTest {

    private SocialWorkerMatchingCommandRepository matchingRepository;
    private MatchingStatusCommandService service;

    @BeforeEach
    void setUp() {

        matchingRepository =
                mock(SocialWorkerMatchingCommandRepository.class);

        service =
                new MatchingStatusCommandService(
                        matchingRepository
                );
    }

    @Test
    @DisplayName("담당 사회복지사는 ACTIVE 매칭을 ENDED로 변경할 수 있다")
    void socialWorkerCanEndOwnMatching() {

        UUID patientId = UUID.randomUUID();
        UUID socialWorkerId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(patientId);

        matchingResult.assign(
                socialWorkerId
        );

        when(
                matchingRepository
                        .findById(
                                matchingResult
                                        .getMatchingResultId()
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        MatchingStatusChangeResult result =
                service.changeStatus(
                        new MatchingStatusChangeCommand(
                                matchingResult
                                        .getMatchingResultId(),
                                "ENDED",
                                socialWorkerId,
                                UserRole.SOCIAL_WORKER
                        )
                );

        assertEquals(
                MatchingStatus.ENDED,
                result.status()
        );

        assertEquals(
                MatchingStatus.ENDED,
                matchingResult.getStatus()
        );
    }

    @Test
    @DisplayName("관리자는 ACTIVE 매칭을 ENDED로 변경할 수 있다")
    void adminCanEndMatching() {

        UUID socialWorkerId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(
                                UUID.randomUUID()
                        );

        matchingResult.assign(
                socialWorkerId
        );

        when(
                matchingRepository
                        .findById(
                                matchingResult
                                        .getMatchingResultId()
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        MatchingStatusChangeResult result =
                service.changeStatus(
                        new MatchingStatusChangeCommand(
                                matchingResult
                                        .getMatchingResultId(),
                                "ENDED",
                                UUID.randomUUID(),
                                UserRole.ADMIN
                        )
                );

        assertEquals(
                MatchingStatus.ENDED,
                result.status()
        );
    }

    @Test
    @DisplayName("담당자가 아닌 사회복지사는 매칭을 종료할 수 없다")
    void otherSocialWorkerCannotEndMatching() {

        UUID assignedWorkerId =
                UUID.randomUUID();

        UUID otherWorkerId =
                UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(
                                UUID.randomUUID()
                        );

        matchingResult.assign(
                assignedWorkerId
        );

        when(
                matchingRepository
                        .findById(
                                matchingResult
                                        .getMatchingResultId()
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.changeStatus(
                                        new MatchingStatusChangeCommand(
                                                matchingResult
                                                        .getMatchingResultId(),
                                                "ENDED",
                                                otherWorkerId,
                                                UserRole.SOCIAL_WORKER
                                        )
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_STATUS_CHANGE_FORBIDDEN,
                exception.getErrorCode()
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                matchingResult.getStatus()
        );
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 매칭은 ENDED로 변경할 수 없다")
    void nonActiveMatchingCannotBeEnded() {

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(
                                UUID.randomUUID()
                        );

        when(
                matchingRepository
                        .findById(
                                matchingResult
                                        .getMatchingResultId()
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.changeStatus(
                                        new MatchingStatusChangeCommand(
                                                matchingResult
                                                        .getMatchingResultId(),
                                                "ENDED",
                                                UUID.randomUUID(),
                                                UserRole.ADMIN
                                        )
                                )
                );

        assertEquals(
                MatchingErrorCode.INVALID_MATCHING_STATUS,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("ENDED 외 상태로 변경을 요청할 수 없다")
    void onlyEndedStatusIsAllowed() {

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.changeStatus(
                                        new MatchingStatusChangeCommand(
                                                UUID.randomUUID(),
                                                "ACTIVE",
                                                UUID.randomUUID(),
                                                UserRole.ADMIN
                                        )
                                )
                );

        assertEquals(
                MatchingErrorCode.INVALID_MATCHING_STATUS,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                matchingRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 매칭 결과는 404 예외를 발생시킨다")
    void matchingNotFound() {

        UUID matchingResultId =
                UUID.randomUUID();

        when(
                matchingRepository
                        .findById(
                                matchingResultId
                        )
        ).thenReturn(
                Optional.empty()
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.changeStatus(
                                        new MatchingStatusChangeCommand(
                                                matchingResultId,
                                                "ENDED",
                                                UUID.randomUUID(),
                                                UserRole.ADMIN
                                        )
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("환자는 사회복지사 매칭을 종료할 수 없다")
    void patientCannotEndMatching() {

        SocialWorkerMatchingResult matchingResult =
                SocialWorkerMatchingResult
                        .requested(
                                UUID.randomUUID()
                        );

        matchingResult.assign(
                UUID.randomUUID()
        );

        when(
                matchingRepository
                        .findById(
                                matchingResult
                                        .getMatchingResultId()
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.changeStatus(
                                        new MatchingStatusChangeCommand(
                                                matchingResult
                                                        .getMatchingResultId(),
                                                "ENDED",
                                                UUID.randomUUID(),
                                                UserRole.PATIENT
                                        )
                                )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_STATUS_CHANGE_FORBIDDEN,
                exception.getErrorCode()
        );
    }
}
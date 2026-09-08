package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingResultCommandServiceTest {

    private SocialWorkerMatchingCommandRepository matchingRepository;
    private MatchingResultCommandService service;

    @BeforeEach
    void setUp() {

        matchingRepository =
                mock(SocialWorkerMatchingCommandRepository.class);

        service =
                new MatchingResultCommandService(
                        matchingRepository
                );
    }

    @Test
    @DisplayName("REQUESTED 매칭을 사회복지사에게 배정하면 ACTIVE 상태가 된다")
    void activateMatching() {

        SocialWorkerMatchingResult matchingResult =
                requestedMatching();

        UUID socialWorkerId =
                UUID.randomUUID();

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

        service.activate(
                matchingResult.getMatchingResultId(),
                socialWorkerId
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                matchingResult.getStatus()
        );

        assertEquals(
                socialWorkerId,
                matchingResult.getSocialWorkerId()
        );

        assertNotNull(
                matchingResult.getAssignedAt()
        );
    }

    @Test
    @DisplayName("REQUESTED가 아닌 매칭은 ACTIVE로 변경할 수 없다")
    void cannotActivateNonRequestedMatching() {

        Instant requestedAt =
                Instant.now();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MatchingStatus.ACTIVE,
                        requestedAt,
                        requestedAt
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

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.activate(
                                matchingResult
                                        .getMatchingResultId(),
                                UUID.randomUUID()
                        )
        );
    }

    @Test
    @DisplayName("REQUESTED 매칭 실패 시 FAILED 상태가 된다")
    void failMatching() {

        SocialWorkerMatchingResult matchingResult =
                requestedMatching();

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

        service.fail(
                matchingResult.getMatchingResultId()
        );

        assertEquals(
                MatchingStatus.FAILED,
                matchingResult.getStatus()
        );

        assertNull(
                matchingResult.getAssignedAt()
        );
    }

    @Test
    @DisplayName("REQUESTED가 아닌 매칭은 FAILED로 변경하지 않는다")
    void nonRequestedMatchingDoesNotFail() {

        Instant requestedAt =
                Instant.now();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MatchingStatus.ACTIVE,
                        requestedAt,
                        requestedAt
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

        service.fail(
                matchingResult.getMatchingResultId()
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                matchingResult.getStatus()
        );
    }

    private SocialWorkerMatchingResult requestedMatching() {

        return new SocialWorkerMatchingResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                MatchingStatus.REQUESTED,
                Instant.now(),
                null
        );
    }
}
package com.todak_todag.social_worker_service.matching.application.service.query;

import com.todak_todag.social_worker_service.global.common.UserRole;
import com.todak_todag.social_worker_service.global.exception.BusinessException;
import com.todak_todag.social_worker_service.matching.application.query.MatchingResultQuery;
import com.todak_todag.social_worker_service.matching.application.result.MatchingResultQueryResult;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.SocialWorkerLoadProjection;
import com.todak_todag.social_worker_service.matching.domain.repository.query.SocialWorkerMatchingQueryRepository;
import com.todak_todag.social_worker_service.matching.exception.MatchingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchingQueryServiceTest {

    private SocialWorkerMatchingQueryRepository matchingRepository;
    private MatchingQueryService matchingQueryService;

    @BeforeEach
    void setUp() {

        matchingRepository =
                mock(SocialWorkerMatchingQueryRepository.class);

        matchingQueryService =
                new MatchingQueryService(
                        matchingRepository
                );
    }

    @Test
    @DisplayName("담당 중인 환자 수가 가장 적은 사회복지사를 선택한다")
    void selectLeastLoadedSocialWorker() {

        UUID workerA =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        UUID workerB =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                );

        UUID workerC =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000003"
                );

        Set<UUID> candidates =
                Set.of(
                        workerA,
                        workerB,
                        workerC
                );

        SocialWorkerLoadProjection loadA =
                mock(SocialWorkerLoadProjection.class);

        SocialWorkerLoadProjection loadB =
                mock(SocialWorkerLoadProjection.class);

        SocialWorkerLoadProjection loadC =
                mock(SocialWorkerLoadProjection.class);

        when(loadA.getSocialWorkerId())
                .thenReturn(workerA);

        when(loadA.getActiveCount())
                .thenReturn(2L);

        when(loadB.getSocialWorkerId())
                .thenReturn(workerB);

        when(loadB.getActiveCount())
                .thenReturn(0L);

        when(loadC.getSocialWorkerId())
                .thenReturn(workerC);

        when(loadC.getActiveCount())
                .thenReturn(1L);

        when(
                matchingRepository
                        .countBySocialWorkerIdsAndStatus(
                                candidates,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        loadA,
                        loadB,
                        loadC
                )
        );

        UUID selected =
                matchingQueryService
                        .select(candidates);

        assertEquals(
                workerB,
                selected
        );
    }

    @Test
    @DisplayName("담당 환자 수가 같으면 사회복지사 ID 오름차순으로 선택한다")
    void selectByUuidWhenLoadIsSame() {

        UUID workerA =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        UUID workerB =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                );

        Set<UUID> candidates =
                Set.of(
                        workerB,
                        workerA
                );

        SocialWorkerLoadProjection loadA =
                mock(SocialWorkerLoadProjection.class);

        SocialWorkerLoadProjection loadB =
                mock(SocialWorkerLoadProjection.class);

        when(loadA.getSocialWorkerId())
                .thenReturn(workerA);

        when(loadA.getActiveCount())
                .thenReturn(1L);

        when(loadB.getSocialWorkerId())
                .thenReturn(workerB);

        when(loadB.getActiveCount())
                .thenReturn(1L);

        when(
                matchingRepository
                        .countBySocialWorkerIdsAndStatus(
                                candidates,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(
                        loadA,
                        loadB
                )
        );

        UUID selected =
                matchingQueryService
                        .select(candidates);

        assertEquals(
                workerA,
                selected
        );
    }

    @Test
    @DisplayName("ACTIVE 매칭 이력이 없는 후보는 담당 환자 수 0명으로 계산한다")
    void workerWithoutActiveMatchingHasZeroLoad() {

        UUID workerA =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                );

        UUID workerB =
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                );

        Set<UUID> candidates =
                Set.of(
                        workerA,
                        workerB
                );

        SocialWorkerLoadProjection loadA =
                mock(SocialWorkerLoadProjection.class);

        when(loadA.getSocialWorkerId())
                .thenReturn(workerA);

        when(loadA.getActiveCount())
                .thenReturn(3L);

        when(
                matchingRepository
                        .countBySocialWorkerIdsAndStatus(
                                candidates,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                List.of(loadA)
        );

        UUID selected =
                matchingQueryService
                        .select(candidates);

        assertEquals(
                workerB,
                selected
        );
    }

    @Test
    @DisplayName("PATIENT는 자신의 매칭 결과를 조회할 수 있다")
    void patientCanGetOwnMatchingResult() {

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

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        patientId,
                        socialWorkerId,
                        MatchingStatus.ACTIVE,
                        requestedAt,
                        assignedAt
                );

        when(
                matchingRepository
                        .findById(
                                matchingResultId
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        MatchingResultQueryResult result =
                matchingQueryService
                        .getResult(
                                new MatchingResultQuery(
                                        matchingResultId,
                                        patientId,
                                        UserRole.PATIENT
                                )
                        );

        assertEquals(
                matchingResultId,
                result.matchingResultId()
        );

        assertEquals(
                patientId,
                result.patientId()
        );

        assertEquals(
                socialWorkerId,
                result.socialWorkerId()
        );

        assertEquals(
                MatchingStatus.ACTIVE,
                result.status()
        );

        assertEquals(
                requestedAt,
                result.requestedAt()
        );

        assertEquals(
                assignedAt,
                result.assignedAt()
        );
    }

    @Test
    @DisplayName("다른 환자의 매칭 결과는 조회할 수 없다")
    void patientCannotGetOtherPatientMatchingResult() {

        UUID matchingResultId = UUID.randomUUID();
        UUID ownerPatientId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        ownerPatientId,
                        null,
                        MatchingStatus.REQUESTED,
                        Instant.now(),
                        null
                );

        when(
                matchingRepository
                        .findById(
                                matchingResultId
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
                                matchingQueryService
                                        .getResult(
                                                new MatchingResultQuery(
                                                        matchingResultId,
                                                        requesterId,
                                                        UserRole.PATIENT
                                                )
                                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("배정된 SOCIAL_WORKER는 자신의 매칭 결과를 조회할 수 있다")
    void assignedSocialWorkerCanGetMatchingResult() {

        UUID matchingResultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID socialWorkerId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        patientId,
                        socialWorkerId,
                        MatchingStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
                );

        when(
                matchingRepository
                        .findById(
                                matchingResultId
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        MatchingResultQueryResult result =
                matchingQueryService
                        .getResult(
                                new MatchingResultQuery(
                                        matchingResultId,
                                        socialWorkerId,
                                        UserRole.SOCIAL_WORKER
                                )
                        );

        assertEquals(
                matchingResultId,
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("배정되지 않은 SOCIAL_WORKER는 매칭 결과를 조회할 수 없다")
    void otherSocialWorkerCannotGetMatchingResult() {

        UUID matchingResultId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID assignedSocialWorkerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        patientId,
                        assignedSocialWorkerId,
                        MatchingStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
                );

        when(
                matchingRepository
                        .findById(
                                matchingResultId
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
                                matchingQueryService
                                        .getResult(
                                                new MatchingResultQuery(
                                                        matchingResultId,
                                                        requesterId,
                                                        UserRole.SOCIAL_WORKER
                                                )
                                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_QUERY_FORBIDDEN,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("ADMIN은 매칭 결과를 조회할 수 있다")
    void adminCanGetMatchingResult() {

        UUID matchingResultId = UUID.randomUUID();

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        MatchingStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
                );

        when(
                matchingRepository
                        .findById(
                                matchingResultId
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        MatchingResultQueryResult result =
                matchingQueryService
                        .getResult(
                                new MatchingResultQuery(
                                        matchingResultId,
                                        UUID.randomUUID(),
                                        UserRole.ADMIN
                                )
                        );

        assertEquals(
                matchingResultId,
                result.matchingResultId()
        );
    }

    @Test
    @DisplayName("존재하지 않는 매칭 결과를 조회하면 예외가 발생한다")
    void matchingResultNotFound() {

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
                                matchingQueryService
                                        .getResult(
                                                new MatchingResultQuery(
                                                        matchingResultId,
                                                        UUID.randomUUID(),
                                                        UserRole.PATIENT
                                                )
                                        )
                );

        assertEquals(
                MatchingErrorCode.MATCHING_RESULT_NOT_FOUND,
                exception.getErrorCode()
        );
    }
}
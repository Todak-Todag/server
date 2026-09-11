package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CarePlanCompletedEventServiceTest {

    private SocialWorkerMatchingCommandRepository matchingCommandRepository;
    private CarePlanCompletedEventService service;

    @BeforeEach
    void setUp() {

        matchingCommandRepository =
                mock(SocialWorkerMatchingCommandRepository.class);

        service =
                new CarePlanCompletedEventService(
                        matchingCommandRepository
                );
    }

    @Test
    @DisplayName("CarePlanCompleted 이벤트 수신 시 ACTIVE 사회복지사 매칭을 ENDED로 변경한다")
    void activeMatchingIsEnded() {

        UUID patientId = UUID.randomUUID();
        UUID matchingResultId = UUID.randomUUID();

        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        patientId,
                        Instant.now()
                );

        SocialWorkerMatchingResult matchingResult =
                new SocialWorkerMatchingResult(
                        matchingResultId,
                        patientId,
                        UUID.randomUUID(),
                        MatchingStatus.ACTIVE,
                        Instant.now(),
                        Instant.now()
                );

        when(
                matchingCommandRepository
                        .findByPatientIdAndStatus(
                                patientId,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                Optional.of(
                        matchingResult
                )
        );

        service.handle(
                event
        );

        assertEquals(
                MatchingStatus.ENDED,
                matchingResult.getStatus()
        );

        verify(
                matchingCommandRepository,
                times(1)
        ).findByPatientIdAndStatus(
                patientId,
                MatchingStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("ACTIVE 사회복지사 매칭이 없으면 상태 변경 없이 정상 종료한다")
    void noActiveMatchingDoesNothing() {

        UUID patientId = UUID.randomUUID();

        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        patientId,
                        Instant.now()
                );

        when(
                matchingCommandRepository
                        .findByPatientIdAndStatus(
                                patientId,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                Optional.empty()
        );

        service.handle(
                event
        );

        verify(
                matchingCommandRepository,
                times(1)
        ).findByPatientIdAndStatus(
                patientId,
                MatchingStatus.ACTIVE
        );

        verifyNoMoreInteractions(
                matchingCommandRepository
        );
    }

    @Test
    @DisplayName("이미 종료된 매칭만 존재하는 경우 중복 이벤트를 수신해도 추가 변경하지 않는다")
    void duplicateEventIsIdempotent() {

        UUID patientId = UUID.randomUUID();

        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        patientId,
                        Instant.now()
                );

        when(
                matchingCommandRepository
                        .findByPatientIdAndStatus(
                                patientId,
                                MatchingStatus.ACTIVE
                        )
        ).thenReturn(
                Optional.empty()
        );

        service.handle(
                event
        );

        service.handle(
                event
        );

        verify(
                matchingCommandRepository,
                times(2)
        ).findByPatientIdAndStatus(
                patientId,
                MatchingStatus.ACTIVE
        );
    }
}
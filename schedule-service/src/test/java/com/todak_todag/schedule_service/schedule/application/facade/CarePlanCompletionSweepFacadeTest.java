package com.todak_todag.schedule_service.schedule.application.facade;

import com.todak_todag.schedule_service.schedule.application.event.CarePlanCompletionEventAppender;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// CarePlanCompleted 보정 스윕 단위 테스트
// 이 클래스의 책임은 "대상을 어떤 기준으로 찾아 누구에게 넘기는가"이며, 발행 여부 판정 자체는 Appender의 몫
@ExtendWith(MockitoExtension.class)
class CarePlanCompletionSweepFacadeTest {

    // CarePlanCompletionSweepFacade.GRACE_PERIOD_DAYS와 동일해야함
    private static final int GRACE_PERIOD_DAYS = 14;

    // CarePlanCompletionSweepFacade.BATCH_SIZE와 동일해야함
    private static final int BATCH_SIZE = 100;

    @Mock
    private ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;

    @Mock
    private CarePlanCompletionEventAppender carePlanCompletionEventAppender;

    @InjectMocks
    private CarePlanCompletionSweepFacade carePlanCompletionSweepFacade;

    @Test
    @DisplayName("마지막 활동일로부터 유예기간이 지난 시점을 임계일로 잡아 대상을 조회한다")
    void 유예기간을_뺀_날짜를_임계일로_조회한다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findSweepTargetCarePlanIds(any(), anyInt()))
                .thenReturn(List.of());

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        ArgumentCaptor<LocalDate> thresholdCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(serviceMatchingAttemptCommandRepository)
                .findSweepTargetCarePlanIds(thresholdCaptor.capture(), eq(BATCH_SIZE));
        assertThat(thresholdCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(GRACE_PERIOD_DAYS));
    }

    @Test
    @DisplayName("조회된 대상마다 Appender의 스윕 진입점을 호출한다 — 적재 안전장치를 실시간 경로와 공유하기 위해")
    void 대상마다_Appender에_위임한다() {
        // given
        UUID carePlanA = UUID.randomUUID();
        UUID carePlanB = UUID.randomUUID();
        when(serviceMatchingAttemptCommandRepository.findSweepTargetCarePlanIds(any(), anyInt()))
                .thenReturn(List.of(carePlanA, carePlanB));

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        verify(carePlanCompletionEventAppender).appendForSweep(carePlanA);
        verify(carePlanCompletionEventAppender).appendForSweep(carePlanB);
    }

    @Test
    @DisplayName("대상이 없으면 Appender를 호출하지 않는다")
    void 대상이_없으면_아무것도_하지_않는다() {
        // given
        when(serviceMatchingAttemptCommandRepository.findSweepTargetCarePlanIds(any(), anyInt()))
                .thenReturn(List.of());

        // when
        carePlanCompletionSweepFacade.sweep();

        // then
        verify(carePlanCompletionEventAppender, never()).appendForSweep(any());
    }

    @Test
    @DisplayName("한 케어플랜 처리가 실패해도 나머지 대상은 계속 처리한다 — 배치 전체가 한 건 때문에 멈추지 않는다")
    void 한_건이_실패해도_나머지를_계속_처리한다() {
        // given
        UUID failing = UUID.randomUUID();
        UUID succeeding = UUID.randomUUID();
        when(serviceMatchingAttemptCommandRepository.findSweepTargetCarePlanIds(any(), anyInt()))
                .thenReturn(List.of(failing, succeeding));
        doThrow(new IllegalStateException("적재 실패"))
                .when(carePlanCompletionEventAppender).appendForSweep(failing);

        // when & then
        assertThatCode(() -> carePlanCompletionSweepFacade.sweep()).doesNotThrowAnyException();
        verify(carePlanCompletionEventAppender).appendForSweep(succeeding);
    }
}

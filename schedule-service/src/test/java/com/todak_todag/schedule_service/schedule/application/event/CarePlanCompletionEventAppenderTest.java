package com.todak_todag.schedule_service.schedule.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ScheduleOutboxEventCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// CarePlanCompleted "발행 조건 판단" 단위 테스트
// 판단 규칙: (1) 진행 중 일정이 0건이고, (2) 이 케어플랜에 CarePlanCompleted가 아직 적재되지 않았을 때만 적재
@ExtendWith(MockitoExtension.class)
class CarePlanCompletionEventAppenderTest {

    // 진행 중으로 간주하는 상태 — CarePlanCompletionEventAppender.UNFINISHED_STATUSES와 동일해야 한다
    private static final List<ScheduleStatus> UNFINISHED_STATUSES =
            List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;

    @Mock
    private ScheduleOutboxEventCommandRepository scheduleOutboxEventCommandRepository;

    // 페이로드가 문서 스펙과 일치하는지 검증해야 하므로 실제 직렬화기를 사용
    @Spy
    private CarePlanCompletedEventPayloadSerializer carePlanCompletedEventPayloadSerializer =
            new CarePlanCompletedEventPayloadSerializer(new ObjectMapper());

    @Mock
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    @InjectMocks
    private CarePlanCompletionEventAppender carePlanCompletionEventAppender;

    @Test
    @DisplayName("케어플랜의 마지막 일정이 정상 수행 완료되면 status=COMPLETED와 해당 결과 ID로 적재한다")
    void 마지막_일정이_수행_완료되면_적재한다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule lastSchedule = schedule(carePlanId, ScheduleStatus.COMPLETED);
        CarePlanServiceResult lastResult = result(lastSchedule.getId());

        givenLastScheduleWithNoUnfinished(carePlanId, lastSchedule);
        when(carePlanServiceResultCommandRepository.findByServiceScheduleId(lastSchedule.getId()))
                .thenReturn(Optional.of(lastResult));

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(lastSchedule);

        // then
        assertThat(capturePayload(carePlanId)).isEqualTo(
                "{\"serviceResultId\":\"" + lastResult.getServiceResultId() + "\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("마지막 일정이 미수행(NO_SHOW)으로 끝나도 결과가 등록되어 있으므로 status=NO_SHOW와 결과 ID로 적재한다")
    void 마지막_일정이_미수행이면_NO_SHOW로_적재한다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule lastSchedule = schedule(carePlanId, ScheduleStatus.NO_SHOW);
        CarePlanServiceResult lastResult = result(lastSchedule.getId());

        givenLastScheduleWithNoUnfinished(carePlanId, lastSchedule);
        when(carePlanServiceResultCommandRepository.findByServiceScheduleId(lastSchedule.getId()))
                .thenReturn(Optional.of(lastResult));

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(lastSchedule);

        // then
        assertThat(capturePayload(carePlanId)).isEqualTo(
                "{\"serviceResultId\":\"" + lastResult.getServiceResultId() + "\",\"status\":\"NO_SHOW\"}"
        );
    }

    @Test
    @DisplayName("재매칭 실패로 마지막 일정이 취소되면 수행 결과가 없으므로 serviceResultId는 null, status는 CANCELED로 적재한다")
    void 마지막_일정이_취소되면_resultId가_null이다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule canceledLastSchedule = schedule(carePlanId, ScheduleStatus.CANCELED);

        givenLastScheduleWithNoUnfinished(carePlanId, canceledLastSchedule);
        when(carePlanServiceResultCommandRepository.findByServiceScheduleId(canceledLastSchedule.getId()))
                .thenReturn(Optional.empty());

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(canceledLastSchedule);

        // then
        assertThat(capturePayload(carePlanId))
                .isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    @Test
    @DisplayName("아직 진행 중인 다른 일정이 남아있으면 적재하지 않는다")
    void 진행_중_일정이_남아있으면_적재하지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule lastSchedule = schedule(carePlanId, ScheduleStatus.COMPLETED);

        when(serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(eq(carePlanId), eq(UNFINISHED_STATUSES)))
                .thenReturn(1L);

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(lastSchedule);

        // then
        verify(carePlanServiceResultCommandRepository, never()).findByServiceScheduleId(any());
        verify(scheduleOutboxCommandService, never()).enqueue(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("이미 적재된 케어플랜이면 다시 적재하지 않는다 — 앞선 일정의 늦은 결과 등록으로 중복 발행되지 않는다")
    void 이미_적재된_케어플랜이면_적재하지_않는다() {
        // given — 진행 중 일정은 없지만 이 케어플랜의 CarePlanCompleted가 이미 아웃박스에 있다
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlierSchedule = schedule(carePlanId, ScheduleStatus.NO_SHOW);

        when(serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(eq(carePlanId), eq(UNFINISHED_STATUSES)))
                .thenReturn(0L);
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                CarePlanCompletedEventPort.EVENT_TYPE, carePlanId)).thenReturn(true);

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(earlierSchedule);

        // then — 페이로드 구성 조회까지 가지 않고 즉시 종료된다
        verify(serviceScheduleCommandRepository, never()).findLastSchedule(any());
        verify(carePlanServiceResultCommandRepository, never()).findByServiceScheduleId(any());
        verify(scheduleOutboxCommandService, never()).enqueue(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("뒤 일정이 먼저 취소되고 앞 일정이 나중에 끝나도 케어플랜 완료 이벤트가 적재된다")
    void 뒤_일정이_먼저_취소되고_앞_일정이_나중에_끝나도_적재한다() {
        // given — B(뒤 일정)가 먼저 취소되고, 그 뒤 A(앞 일정)의 수행 결과가 등록되는 상황
        //         B 취소 시점에는 A가 SCHEDULED라 적재되지 않았고, 이제 A가 마지막 트리거다
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlierA = schedule(carePlanId, ScheduleStatus.COMPLETED);
        ServiceSchedule laterB = schedule(carePlanId, ScheduleStatus.CANCELED);

        givenLastScheduleWithNoUnfinished(carePlanId, laterB);
        when(carePlanServiceResultCommandRepository.findByServiceScheduleId(laterB.getId()))
                .thenReturn(Optional.empty());

        // when — 트리거는 A(마지막 일정이 아님)
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(earlierA);

        // then — 케어플랜에 남은 일정이 없으므로 적재되고, 페이로드는 마지막 일정(B) 기준이다
        assertThat(capturePayload(carePlanId))
                .isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    // 진행 중 일정이 0건이고, 아직 적재된 적이 없는 상황
    private void givenLastScheduleWithNoUnfinished(UUID carePlanId, ServiceSchedule lastSchedule) {
        when(serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(eq(carePlanId), eq(UNFINISHED_STATUSES)))
                .thenReturn(0L);
        when(scheduleOutboxEventCommandRepository.existsByEventTypeAndAggregateId(
                CarePlanCompletedEventPort.EVENT_TYPE, carePlanId)).thenReturn(false);
        when(serviceScheduleCommandRepository.findLastSchedule(carePlanId)).thenReturn(Optional.of(lastSchedule));
    }

    // 아웃박스에 적재된 payload를 꺼낸다 — aggregateId가 carePlanId인지도 함께 검증
    private String capturePayload(UUID carePlanId) {
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(scheduleOutboxCommandService).enqueue(
                eq(CarePlanCompletedEventPort.EVENT_TYPE),
                eq(carePlanId),
                payloadCaptor.capture()
        );

        return payloadCaptor.getValue();
    }

    private ServiceSchedule schedule(UUID carePlanId, ScheduleStatus status) {
        LocalDate date = LocalDate.now().plusDays(3);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        setField(ServiceSchedule.class, schedule, "id", UUID.randomUUID());
        setField(ServiceSchedule.class, schedule, "status", status);
        return schedule;
    }

    private CarePlanServiceResult result(UUID serviceScheduleId) {
        CarePlanServiceResult result = CarePlanServiceResult.record(
                serviceScheduleId,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                null
        );
        setField(CarePlanServiceResult.class, result, "serviceResultId", UUID.randomUUID());
        return result;
    }

    private void setField(Class<?> type, Object target, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

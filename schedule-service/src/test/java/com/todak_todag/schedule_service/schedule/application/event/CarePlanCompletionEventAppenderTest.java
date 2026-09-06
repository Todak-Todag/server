package com.todak_todag.schedule_service.schedule.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanCompletedEventPort;
import com.todak_todag.schedule_service.schedule.application.service.command.ScheduleOutboxCommandService;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
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
// 판단 규칙: (1) 이번에 결말난 일정이 케어플랜의 마지막 일정이고, (2) 진행 중 일정이 0건일 때만 적재
@ExtendWith(MockitoExtension.class)
class CarePlanCompletionEventAppenderTest {

    // 진행 중으로 간주하는 상태 — CarePlanCompletionEventAppender.UNFINISHED_STATUSES와 동일해야 한다
    private static final List<ScheduleStatus> UNFINISHED_STATUSES =
            List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.RESCHEDULING);

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;

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

        when(serviceScheduleCommandRepository.findLastSchedule(carePlanId)).thenReturn(Optional.of(lastSchedule));
        when(serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(eq(carePlanId), eq(UNFINISHED_STATUSES)))
                .thenReturn(1L);

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(lastSchedule);

        // then
        verify(carePlanServiceResultCommandRepository, never()).findByServiceScheduleId(any());
        verify(scheduleOutboxCommandService, never()).enqueue(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("이번에 결말난 일정이 마지막 일정이 아니면 적재하지 않는다 — 앞선 일정의 늦은 결과 등록으로 중복 발행되지 않는다")
    void 마지막_일정이_아니면_적재하지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceSchedule earlierSchedule = schedule(carePlanId, ScheduleStatus.NO_SHOW);
        ServiceSchedule lastSchedule = schedule(carePlanId, ScheduleStatus.CANCELED);

        when(serviceScheduleCommandRepository.findLastSchedule(carePlanId)).thenReturn(Optional.of(lastSchedule));

        // when
        carePlanCompletionEventAppender.appendIfCarePlanCompleted(earlierSchedule);

        // then
        verify(serviceScheduleCommandRepository, never()).countByCarePlanIdAndStatusIn(any(), any());
        verify(scheduleOutboxCommandService, never()).enqueue(anyString(), any(), anyString());
    }

    // 마지막 일정이면서 진행 중 일정이 0건인 상황
    private void givenLastScheduleWithNoUnfinished(UUID carePlanId, ServiceSchedule lastSchedule) {
        when(serviceScheduleCommandRepository.findLastSchedule(carePlanId)).thenReturn(Optional.of(lastSchedule));
        when(serviceScheduleCommandRepository.countByCarePlanIdAndStatusIn(eq(carePlanId), eq(UNFINISHED_STATUSES)))
                .thenReturn(0L);
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

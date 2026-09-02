package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Care Plan 조회는 ServiceScheduleFacade가 트랜잭션 밖에서 미리 수행해 carePlanRange 파라미터로 전달하므로
// 이 서비스는 CarePlanPort를 직접 호출하지 않는다 (오케스트레이션 검증은 ServiceScheduleFacadeTest 참고).
// ProviderReMatched 이벤트는 실제 브로커로 즉시 발행하지 않고 ScheduleOutboxCommandService.enqueue()로
// 같은 트랜잭션 안에서 아웃박스에 적재하기만 한다 (아웃박스 패턴). 실제 발행은 ScheduleOutboxRelayFacade 참고.
@ExtendWith(MockitoExtension.class)
class ServiceScheduleCommandServiceTest {

    private static final String SERIALIZED_PAYLOAD = "{\"serialized\":true}";

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    @Mock
    private ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;

    @InjectMocks
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Test
    void 하루_앞당기기_요청은_RESCHEDULING으로_변경되고_아웃박스에_이벤트를_적재한다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.minusDays(1);
        ServiceSchedule schedule = confirmedSchedule(currentDate);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));
        when(serviceScheduleCommandRepository.save(schedule)).thenReturn(schedule);
        when(providerReMatchEventPayloadSerializer.serialize(any())).thenReturn(SERIALIZED_PAYLOAD);

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when
        ServiceScheduleRescheduleResult result = serviceScheduleCommandService.reschedule(command, carePlanRange);

        // then
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.RESCHEDULING);
        assertThat(result.status()).isEqualTo(ScheduleStatus.RESCHEDULING);
        verify(providerReMatchEventPayloadSerializer).serialize(
                new ProviderReMatchEventPort.ProviderReMatchEvent(schedule.getId(), schedule.getServiceOfferingId(), requestedDate)
        );
        verify(scheduleOutboxCommandService).enqueue(ProviderReMatchEventPort.EVENT_TYPE, schedule.getId(), SERIALIZED_PAYLOAD);
    }

    @Test
    void 하루_미루기_요청은_RESCHEDULING으로_변경되고_아웃박스에_이벤트를_적재한다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.plusDays(1);
        ServiceSchedule schedule = confirmedSchedule(currentDate);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));
        when(serviceScheduleCommandRepository.save(schedule)).thenReturn(schedule);
        when(providerReMatchEventPayloadSerializer.serialize(any())).thenReturn(SERIALIZED_PAYLOAD);

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when
        ServiceScheduleRescheduleResult result = serviceScheduleCommandService.reschedule(command, carePlanRange);

        // then
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.RESCHEDULING);
        assertThat(result.status()).isEqualTo(ScheduleStatus.RESCHEDULING);
        verify(scheduleOutboxCommandService).enqueue(ProviderReMatchEventPort.EVENT_TYPE, schedule.getId(), SERIALIZED_PAYLOAD);
    }

    @Test
    void 당일_일정으로_앞당기려_하면_400을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        // currentDate가 내일이면 하루 앞당긴 날짜(D-1)가 오늘이 된다
        LocalDate currentDate = LocalDate.now().plusDays(1);
        LocalDate requestedDate = LocalDate.now();
        ServiceSchedule schedule = confirmedSchedule(currentDate);
        // 24시간 전 데드라인 검증에 걸리지 않도록 시작 시각을 충분히 미래로 고정 (이 테스트의 관심사는 "당일 변경" 규칙 하나뿐)
        setStartedAt(schedule, LocalDateTime.now().plusHours(48));

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_TO_TODAY_NOT_ALLOWED);
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void Care_Plan_일정_범위를_초과하는_하루_미루기는_400을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.plusDays(1);
        LocalDate finishDate = currentDate; // requestedDate(D+1)가 finishDate를 초과하도록 설정
        ServiceSchedule schedule = confirmedSchedule(currentDate);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), finishDate, patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_RESCHEDULE_EXCEEDS_CARE_PLAN_RANGE);
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void 기존_날짜_기준_하루_전후가_아니면_400을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.plusDays(2);
        ServiceSchedule schedule = confirmedSchedule(currentDate);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_RESCHEDULE_DATE);
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void status가_SCHEDULED가_아니면_400을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.minusDays(1);
        ServiceSchedule schedule = confirmedSchedule(currentDate);
        setStatus(schedule, ScheduleStatus.COMPLETED);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_RESCHEDULING);
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void 일정_시작_24시간_이내_요청이면_400을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.minusDays(1);
        ServiceSchedule schedule = confirmedSchedule(currentDate);
        setStartedAt(schedule, LocalDateTime.now().plusHours(2));

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, patientId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_DELAY_DEADLINE_EXCEEDED);
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void 본인_소유가_아닌_일정이면_403을_던진다() {
        // given
        UUID patientId = UUID.randomUUID();
        UUID otherRequesterId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.now().plusDays(3);
        LocalDate requestedDate = currentDate.minusDays(1);
        ServiceSchedule schedule = confirmedSchedule(currentDate);

        when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(schedule.getId(), requestedDate, otherRequesterId);
        CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.AUTH_FORBIDDEN);
        verify(serviceScheduleCommandRepository, never()).save(any());
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    @Test
    void 존재하지_않는_일정이면_404를_던진다() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.empty());

        ServiceScheduleRescheduleCommand command = new ServiceScheduleRescheduleCommand(
                serviceScheduleId, LocalDate.now().plusDays(1), UUID.randomUUID()
        );
        CarePlanPort.CarePlanRange carePlanRange =
                new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.now().plusDays(10), UUID.randomUUID());

        // when & then
        assertThatThrownBy(() -> serviceScheduleCommandService.reschedule(command, carePlanRange))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND);
        verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
    }

    private ServiceSchedule confirmedSchedule(LocalDate date) {
        return ServiceSchedule.confirm(
                UUID.randomUUID(),
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
    }

    private void setStatus(ServiceSchedule schedule, ScheduleStatus status) {
        try {
            Field field = ServiceSchedule.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(schedule, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setStartedAt(ServiceSchedule schedule, LocalDateTime startedAt) {
        try {
            Field field = ServiceSchedule.class.getDeclaredField("startedAt");
            field.setAccessible(true);
            field.set(schedule, startedAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

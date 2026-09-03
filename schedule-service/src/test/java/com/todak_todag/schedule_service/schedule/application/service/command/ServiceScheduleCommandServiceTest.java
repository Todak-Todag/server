package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCancelCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompleteCommand;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleCompletionStatus;
import com.todak_todag.schedule_service.schedule.application.command.ServiceScheduleRescheduleCommand;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCancelResult;
import com.todak_todag.schedule_service.schedule.application.port.CarePlanPort;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleCompleteResult;
import com.todak_todag.schedule_service.schedule.application.result.ServiceScheduleRescheduleResult;
import com.todak_todag.schedule_service.schedule.application.support.ProviderReMatchEventPayloadSerializer;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

@ExtendWith(MockitoExtension.class)
class ServiceScheduleCommandServiceTest {

    private static final String SERIALIZED_PAYLOAD = "{\"serialized\":true}";

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private ScheduleOutboxCommandService scheduleOutboxCommandService;

    @Mock
    private ProviderReMatchEventPayloadSerializer providerReMatchEventPayloadSerializer;

    @Spy
    private ServiceScheduleValidator serviceScheduleValidator = new ServiceScheduleValidator();

    @InjectMocks
    private ServiceScheduleCommandService serviceScheduleCommandService;

    @Nested
    @DisplayName("서비스 일정 변경")
    class rescheduleTest {
        @Test
        @DisplayName("하루 앞당기기 요청은 RESCHEDULING으로 변경되고 아웃박스에 이벤트를 적재한다")
        void reschedule_dayBefore_success() {
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
        @DisplayName("하루 미루기 요청은 RESCHEDULING으로 변경되고 아웃박스에 이벤트를 적재한다")
        void reschedule_dayAfter_success() {
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
        @DisplayName("당일 일정으로 앞당기려 하면 400을 던진다")
        void reschedule_toToday_badRequest() {
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
        @DisplayName("Care Plan 일정 범위를 초과하는 하루 미루기는 400을 던진다")
        void reschedule_exceedsCarePlanRange_badRequest() {
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
        @DisplayName("기존 날짜 기준 하루 전후가 아니면 400을 던진다")
        void reschedule_notAdjacentDay_badRequest() {
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
        @DisplayName("status가 SCHEDULED가 아니면 400을 던진다")
        void reschedule_invalidStatus_badRequest() {
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
        @DisplayName("일정 시작 24시간 이내 요청이면 400을 던진다")
        void reschedule_withinDeadline_badRequest() {
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
        @DisplayName("본인 소유가 아닌 일정이면 403을 던진다")
        void reschedule_notOwner_forbidden() {
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
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
            verify(serviceScheduleCommandRepository, never()).save(any());
            verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던진다 (리소스 존재 비노출)")
        void reschedule_notFound_forbidden() {
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
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
            verify(scheduleOutboxCommandService, never()).enqueue(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("서비스 일정 취소")
    class cancelTest {
        @Test
        @DisplayName("정상 취소 요청은 CANCELED로 변경되고 취소 사유와 취소 일시가 기록된다")
        void cancel_success() {
            // given
            UUID patientId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));
            when(serviceScheduleCommandRepository.save(schedule)).thenReturn(schedule);

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(schedule.getId(), "개인 사정으로 취소합니다", patientId);
            CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

            // when
            ServiceScheduleCancelResult result = serviceScheduleCommandService.cancel(command, carePlanRange);

            // then
            assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.CANCELED);
            assertThat(schedule.getCancelReason()).isEqualTo("개인 사정으로 취소합니다");
            assertThat(schedule.getCanceledAt()).isNotNull();
            assertThat(result.serviceScheduleId()).isEqualTo(schedule.getId());
            assertThat(result.canceledAt()).isEqualTo(schedule.getCanceledAt());
        }

        @Test
        @DisplayName("이미 완료된 일정이면 409를 던진다")
        void cancel_alreadyCompleted_conflict() {
            // given
            UUID patientId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setStatus(schedule, ScheduleStatus.COMPLETED);

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(schedule.getId(), "취소 사유", patientId);
            CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.cancel(command, carePlanRange))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 취소된 일정을 재취소하면 409를 던진다")
        void cancel_alreadyCanceled_conflict() {
            // given
            UUID patientId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setStatus(schedule, ScheduleStatus.CANCELED);

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(schedule.getId(), "취소 사유", patientId);
            CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.cancel(command, carePlanRange))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_CANCEL);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("일정 시작 24시간 이내 취소 요청이면 409를 던진다")
        void cancel_withinDeadline_conflict() {
            // given
            UUID patientId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setStartedAt(schedule, LocalDateTime.now().plusHours(2));

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(schedule.getId(), "취소 사유", patientId);
            CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.cancel(command, carePlanRange))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_CANCEL_DEADLINE_EXCEEDED);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인 소유가 아닌 일정이면 403을 던진다")
        void cancel_notOwner_forbidden() {
            // given
            UUID patientId = UUID.randomUUID();
            UUID otherRequesterId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(schedule.getId(), "취소 사유", otherRequesterId);
            CarePlanPort.CarePlanRange carePlanRange = new CarePlanPort.CarePlanRange(UUID.randomUUID(), currentDate.plusDays(10), patientId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.cancel(command, carePlanRange))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던진다 (리소스 존재 비노출)")
        void cancel_notFound_forbidden() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.empty());

            ServiceScheduleCancelCommand command = new ServiceScheduleCancelCommand(
                    serviceScheduleId, "취소 사유", UUID.randomUUID()
            );
            CarePlanPort.CarePlanRange carePlanRange =
                    new CarePlanPort.CarePlanRange(UUID.randomUUID(), LocalDate.now().plusDays(10), UUID.randomUUID());

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.cancel(command, carePlanRange))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("서비스 수행 완료")
    class completeTest {
        @Test
        @DisplayName("정상 완료(COMPLETED) 요청은 COMPLETED로 변경된다")
        void complete_completed_success() {
            // given
            UUID providerId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setFinishedAt(schedule, LocalDateTime.now().minusHours(1));

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));
            when(serviceScheduleCommandRepository.save(schedule)).thenReturn(schedule);

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(schedule.getId(), ServiceScheduleCompletionStatus.COMPLETED, providerId);

            // when
            ServiceScheduleCompleteResult result = serviceScheduleCommandService.complete(command, providerId);

            // then
            assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.COMPLETED);
            assertThat(result.status()).isEqualTo(ScheduleStatus.COMPLETED);
        }

        @Test
        @DisplayName("정상 미완료(NO_SHOW) 요청은 NO_SHOW로 변경된다")
        void complete_noShow_success() {
            // given
            UUID providerId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setFinishedAt(schedule, LocalDateTime.now().minusHours(1));

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));
            when(serviceScheduleCommandRepository.save(schedule)).thenReturn(schedule);

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(schedule.getId(), ServiceScheduleCompletionStatus.NO_SHOW, providerId);

            // when
            ServiceScheduleCompleteResult result = serviceScheduleCommandService.complete(command, providerId);

            // then
            assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.NO_SHOW);
            assertThat(result.status()).isEqualTo(ScheduleStatus.NO_SHOW);
        }

        @Test
        @DisplayName("status가 SCHEDULED가 아니면 409를 던진다")
        void complete_invalidStatus_conflict() {
            // given
            UUID providerId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setFinishedAt(schedule, LocalDateTime.now().minusHours(1));
            setStatus(schedule, ScheduleStatus.CANCELED);

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(schedule.getId(), ServiceScheduleCompletionStatus.COMPLETED, providerId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.complete(command, providerId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_INVALID_STATUS_FOR_COMPLETED);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인이 배정된 서비스 제공자가 아니면 403을 던진다")
        void complete_notAssignedProvider_forbidden() {
            // given
            UUID assignedProviderId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setFinishedAt(schedule, LocalDateTime.now().minusHours(1));

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(schedule.getId(), ServiceScheduleCompletionStatus.COMPLETED, requesterId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.complete(command, assignedProviderId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("finishedAt 이전 요청이면 400을 던진다")
        void complete_beforeFinishedAt_badRequest() {
            // given
            UUID providerId = UUID.randomUUID();
            LocalDate currentDate = LocalDate.now().plusDays(3);
            ServiceSchedule schedule = confirmedSchedule(currentDate);
            setFinishedAt(schedule, LocalDateTime.now().plusHours(1));

            when(serviceScheduleCommandRepository.findById(any())).thenReturn(Optional.of(schedule));

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(schedule.getId(), ServiceScheduleCompletionStatus.COMPLETED, providerId);

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.complete(command, providerId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_STATUS_UPDATE_TOO_EARLY);
            verify(serviceScheduleCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 403을 던진다 (리소스 존재 비노출)")
        void complete_notFound_forbidden() {
            // given
            UUID serviceScheduleId = UUID.randomUUID();
            when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.empty());

            ServiceScheduleCompleteCommand command =
                    new ServiceScheduleCompleteCommand(serviceScheduleId, ServiceScheduleCompletionStatus.COMPLETED, UUID.randomUUID());

            // when & then
            assertThatThrownBy(() -> serviceScheduleCommandService.complete(command, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        }
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

    private void setFinishedAt(ServiceSchedule schedule, LocalDateTime finishedAt) {
        try {
            Field field = ServiceSchedule.class.getDeclaredField("finishedAt");
            field.setAccessible(true);
            field.set(schedule, finishedAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

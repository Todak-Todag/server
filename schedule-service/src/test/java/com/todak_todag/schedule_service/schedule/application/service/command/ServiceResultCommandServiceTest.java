package com.todak_todag.schedule_service.schedule.application.service.command;

import com.todak_todag.schedule_service.global.exception.BusinessException;
import com.todak_todag.schedule_service.global.exception.CommonErrorCode;
import com.todak_todag.schedule_service.global.exception.ScheduleErrorCode;
import com.todak_todag.schedule_service.schedule.application.command.ServiceResultRegisterCommand;
import com.todak_todag.schedule_service.schedule.application.result.ServiceResultRegisterResult;
import com.todak_todag.schedule_service.schedule.application.support.ServiceScheduleValidator;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanServiceResultCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import org.junit.jupiter.api.DisplayName;
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
class ServiceResultCommandServiceTest {

    @Mock
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Mock
    private CarePlanServiceResultCommandRepository carePlanServiceResultCommandRepository;

    @Spy
    private ServiceScheduleValidator serviceScheduleValidator = new ServiceScheduleValidator();

    @InjectMocks
    private ServiceResultCommandService serviceResultCommandService;

    @Test
    @DisplayName("status가 COMPLETED인 일정에 정상 등록되면 serviceResultId를 반환한다")
    void register_completedSchedule_success() {
        // given
        UUID providerId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule();
        setStatus(schedule, ScheduleStatus.COMPLETED);
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime finishedAt = LocalDateTime.now().minusHours(1);
        UUID serviceScheduleId = UUID.randomUUID();

        when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.of(schedule));
        when(carePlanServiceResultCommandRepository.existsByServiceScheduleId(serviceScheduleId)).thenReturn(false);
        when(carePlanServiceResultCommandRepository.save(any(CarePlanServiceResult.class)))
                .thenAnswer(invocation -> stampGeneratedId(invocation.getArgument(0)));

        ServiceResultRegisterCommand command =
                new ServiceResultRegisterCommand(serviceScheduleId, startedAt, finishedAt, "정상적으로 서비스 제공 완료", providerId);

        // when
        ServiceResultRegisterResult result = serviceResultCommandService.register(command, providerId);

        // then
        assertThat(result.serviceResultId()).isNotNull();
        verify(carePlanServiceResultCommandRepository).save(any(CarePlanServiceResult.class));
    }

    @Test
    @DisplayName("status가 NO_SHOW인 일정에도 정상 등록된다")
    void register_noShowSchedule_success() {
        // given
        UUID providerId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule();
        setStatus(schedule, ScheduleStatus.NO_SHOW);
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime finishedAt = LocalDateTime.now().minusHours(1);
        UUID serviceScheduleId = UUID.randomUUID();

        when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.of(schedule));
        when(carePlanServiceResultCommandRepository.existsByServiceScheduleId(serviceScheduleId)).thenReturn(false);
        when(carePlanServiceResultCommandRepository.save(any(CarePlanServiceResult.class)))
                .thenAnswer(invocation -> stampGeneratedId(invocation.getArgument(0)));

        ServiceResultRegisterCommand command =
                new ServiceResultRegisterCommand(serviceScheduleId, startedAt, finishedAt, "예약 부도", providerId);

        // when
        ServiceResultRegisterResult result = serviceResultCommandService.register(command, providerId);

        // then
        assertThat(result.serviceResultId()).isNotNull();
    }

    @Test
    @DisplayName("note 없이도 정상 등록된다 (note는 선택값)")
    void register_withoutNote_success() {
        // given
        UUID providerId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule();
        setStatus(schedule, ScheduleStatus.COMPLETED);
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime finishedAt = LocalDateTime.now().minusHours(1);
        UUID serviceScheduleId = UUID.randomUUID();

        when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.of(schedule));
        when(carePlanServiceResultCommandRepository.existsByServiceScheduleId(serviceScheduleId)).thenReturn(false);
        when(carePlanServiceResultCommandRepository.save(any(CarePlanServiceResult.class)))
                .thenAnswer(invocation -> stampGeneratedId(invocation.getArgument(0)));

        ServiceResultRegisterCommand command =
                new ServiceResultRegisterCommand(serviceScheduleId, startedAt, finishedAt, null, providerId);

        // when
        ServiceResultRegisterResult result = serviceResultCommandService.register(command, providerId);

        // then
        assertThat(result.serviceResultId()).isNotNull();
    }

    @Test
    @DisplayName("status가 SCHEDULED(COMPLETED/NO_SHOW가 아님)이면 409를 던진다")
    void register_notCompletedOrNoShow_conflict() {
        // given
        UUID providerId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule(); // 기본 상태 SCHEDULED

        when(serviceScheduleCommandRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                schedule.getId(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, providerId
        );

        // when & then
        assertThatThrownBy(() -> serviceResultCommandService.register(command, providerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_INVALID_SCHEDULE_STATUS);
        verify(carePlanServiceResultCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 결과가 등록된 일정에 재등록을 시도하면 409를 던진다")
    void register_alreadyExists_conflict() {
        // given
        UUID providerId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule();
        setStatus(schedule, ScheduleStatus.COMPLETED);

        when(serviceScheduleCommandRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(carePlanServiceResultCommandRepository.existsByServiceScheduleId(schedule.getId())).thenReturn(true);

        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                schedule.getId(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, providerId
        );

        // when & then
        assertThatThrownBy(() -> serviceResultCommandService.register(command, providerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_RESULTS_ALREADY_EXISTS);
        verify(carePlanServiceResultCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인이 배정된 서비스 제공자가 아니면 403을 던진다")
    void register_notAssignedProvider_forbidden() {
        // given
        UUID assignedProviderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ServiceSchedule schedule = confirmedSchedule();
        setStatus(schedule, ScheduleStatus.COMPLETED);

        when(serviceScheduleCommandRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                schedule.getId(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, requesterId
        );

        // when & then
        assertThatThrownBy(() -> serviceResultCommandService.register(command, assignedProviderId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CommonErrorCode.AUTH_FORBIDDEN);
        verify(carePlanServiceResultCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 serviceScheduleId면 404를 던진다 (07번 문서는 05번과 달리 리소스 존재를 비노출하지 않음)")
    void register_notFound_notFound() {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        when(serviceScheduleCommandRepository.findById(serviceScheduleId)).thenReturn(Optional.empty());

        ServiceResultRegisterCommand command = new ServiceResultRegisterCommand(
                serviceScheduleId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), null, UUID.randomUUID()
        );

        // when & then
        assertThatThrownBy(() -> serviceResultCommandService.register(command, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ScheduleErrorCode.SERVICE_SCHEDULE_NOT_FOUND);
        verify(carePlanServiceResultCommandRepository, never()).save(any());
    }

    private ServiceSchedule confirmedSchedule() {
        LocalDate date = LocalDate.now().plusDays(3);
        return ServiceSchedule.confirm(
                UUID.randomUUID(),
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

    private CarePlanServiceResult stampGeneratedId(CarePlanServiceResult carePlanServiceResult) {
        try {
            Field field = CarePlanServiceResult.class.getDeclaredField("serviceResultId");
            field.setAccessible(true);
            field.set(carePlanServiceResult, UUID.randomUUID());
            return carePlanServiceResult;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

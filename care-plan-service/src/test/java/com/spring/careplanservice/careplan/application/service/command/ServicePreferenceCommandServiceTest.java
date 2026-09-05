package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.application.command.ServicePreferenceUpdateCommand;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceUpdateResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.application.support.ServicePreferenceDateValidator;
import com.spring.careplanservice.careplan.domain.entity.*;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicePreferenceCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();
    UUID planServiceId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();
    LocalDate preferredDate = LocalDate.of(2026, 9, 1);

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Mock
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

    @InjectMocks
    private ServicePreferenceCommandService servicePreferenceCommandService;

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Spy
    private CarePlanOwnerValidator carePlanOwnerValidator;

    @Mock
    private ServicePreferenceDateValidator servicePreferenceDateValidator;

    @Nested
    @DisplayName("서비스 희망 일정 생성")
    class CreateServicePreference {
        @Test
        @DisplayName("성공")
        void createServicePreference_success() {
            LocalDate preferredDate = LocalDate.of(2026, 9, 10);

            ServicePreferenceCreateCommand servicePreferenceCreateCommand = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    preferredDate,
                    PreferredTimeSlot.MORNING
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanServicePreference carePlanServicePreference = mock(CarePlanServicePreference.class);

            UUID servicePreferenceId = UUID.randomUUID();

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceCommandRepository.save(any(CarePlanServicePreference.class))).willReturn(carePlanServicePreference);
            given(carePlanServicePreference.getId()).willReturn(servicePreferenceId);

            ServicePreferenceCreateResult result = servicePreferenceCommandService.createServicePreference(servicePreferenceCreateCommand);

            ArgumentCaptor<CarePlanServicePreference> captor = ArgumentCaptor.forClass(CarePlanServicePreference.class);

            verify(servicePreferenceCommandRepository).save(captor.capture());

            CarePlanServicePreference preference = captor.getValue();

            assertThat(preference.getPlanServiceId()).isEqualTo(planServiceId);
            assertThat(preference.getPreferredDate()).isEqualTo(preferredDate);
            assertThat(preference.getPreferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);
            assertThat(result.servicePreferenceId()).isEqualTo(servicePreferenceId);
        }

        @Test
        @DisplayName("Care Plan 서비스가 존재하지 않으면 예외")
        void createServicePreference_planService_notFound() {
            ServicePreferenceCreateCommand command = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceCommandService.createServicePreference(command))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).findById(any(UUID.class));
            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void createServicePreference_carePlanNotFound() {
            ServicePreferenceCreateCommand command = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceCommandService.createServicePreference(command))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void createServicePreference_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceCreateCommand command = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    otherPatientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> servicePreferenceCommandService.createServicePreference(command))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }

        @Test
        @DisplayName("희망 날짜 검증에 실패하면 예외")
        void createServicePreference_invalidPreferredDate() {
            ServicePreferenceCreateCommand servicePreferenceCreateCommand = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    preferredDate,
                    PreferredTimeSlot.MORNING
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            doThrow(new BusinessException(ErrorCode.SERVICE_PREFERENCE_DATE_OUT_OF_RANGE)).when(servicePreferenceDateValidator).validate(
                    preferredDate,
                    carePlan.getStartDate(),
                    carePlan.getFinishDate()
            );
            assertThatThrownBy(() -> servicePreferenceCommandService.createServicePreference(servicePreferenceCreateCommand))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.SERVICE_PREFERENCE_DATE_OUT_OF_RANGE
                    );

            verify(servicePreferenceDateValidator).validate(
                    preferredDate,
                    carePlan.getStartDate(),
                    carePlan.getFinishDate()
            );
            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }

        @Test
        @DisplayName("Care Plan이 검토 중 상태가 아니면 예외")
        void createServicePreference_invalidCarePlanStatus() {
            ServicePreferenceCreateCommand servicePreferenceCreateCommand = new ServicePreferenceCreateCommand(
                    patientId,
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            ReflectionTestUtils.setField(
                    carePlan,
                    "status",
                    CarePlanStatus.CONFIRMED
            );

            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> servicePreferenceCommandService.createServicePreference(servicePreferenceCreateCommand))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }
    }

    @Nested
    @DisplayName("서비스 희망 일정 수정")
    class UpdateServicePreference {
        UUID servicePreferenceId = UUID.randomUUID();

        @Test
        @DisplayName("성공")
        void updateServicePreference_success() {
            LocalDate preferredDate = LocalDate.of(2026, 9, 15);

            ServicePreferenceUpdateCommand command = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    preferredDate,
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );
            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            ServicePreferenceUpdateResult servicePreferenceUpdateResult = servicePreferenceCommandService.updateServicePreference(command);

            assertThat(preference.getPreferredDate()).isEqualTo(preferredDate);
            assertThat(preference.getPreferredTimeSlot()).isEqualTo(PreferredTimeSlot.AFTERNOON);
            assertThat(servicePreferenceUpdateResult.servicePreferenceId()).isEqualTo(preference.getId());

            verify(servicePreferenceCommandRepository, never()).save(any(CarePlanServicePreference.class));
        }

        @Test
        @DisplayName("서비스 희망 일정이 존재하지 않으면 예외")
        void updateServicePreference_notFound() {
            ServicePreferenceUpdateCommand command = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    LocalDate.of(2026, 9, 15),
                    PreferredTimeSlot.AFTERNOON
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceCommandService.updateServicePreference(command))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void updateServicePreference_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceUpdateCommand command = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    LocalDate.of(2026, 9, 15),
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );
            CarePlan carePlan = CarePlan.create(
                    otherPatientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );
            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> servicePreferenceCommandService.updateServicePreference(command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Care Plan이 검토 중 상태가 아니면 예외")
        void updateServicePreference_invalidCarePlanStatus() {
            ServicePreferenceUpdateCommand command = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    LocalDate.of(2026, 9, 15),
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );
            ReflectionTestUtils.setField(
                    carePlan,
                    "status",
                    CarePlanStatus.CONFIRMED
            );
            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> servicePreferenceCommandService.updateServicePreference(command))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("희망 날짜 검증에 실패하면 예외")
        void updateServicePreference_invalidPreferredDate() {
            ServicePreferenceUpdateCommand servicePreferenceUpdateCommand = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    preferredDate,
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanServicePreference carePlanServicePreference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.of(carePlanServicePreference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            doThrow(new BusinessException(ErrorCode.SERVICE_PREFERENCE_DATE_OUT_OF_RANGE)).when(servicePreferenceDateValidator).validate(
                    preferredDate,
                    carePlan.getStartDate(),
                    carePlan.getFinishDate()
            );
            assertThatThrownBy(() -> servicePreferenceCommandService.updateServicePreference(servicePreferenceUpdateCommand))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.SERVICE_PREFERENCE_DATE_OUT_OF_RANGE
                    );

            verify(servicePreferenceDateValidator).validate(
                    preferredDate,
                    carePlan.getStartDate(),
                    carePlan.getFinishDate()
            );
        }

        @Test
        @DisplayName("Care Plan 서비스가 존재하지 않으면 예외")
        void updateServicePreference_planServiceNotFound() {
            ServicePreferenceUpdateCommand servicePreferenceUpdateCommand = new ServicePreferenceUpdateCommand(
                    patientId,
                    servicePreferenceId,
                    LocalDate.of(2026, 9, 15),
                    PreferredTimeSlot.AFTERNOON
            );

            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceCommandRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceCommandService.updateServicePreference(servicePreferenceUpdateCommand))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode",
                            ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                    );
            verify(carePlanCommandRepository, never()).findById(any(UUID.class));
        }
    }
}
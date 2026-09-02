package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.ServicePreferenceCreateCommand;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceCreateResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

    @Spy
    private CarePlanOwnerValidator carePlanOwnerValidator;

    @InjectMocks
    private ServicePreferenceCommandService servicePreferenceCommandService;

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

        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void createServicePreference_forbidden() {

        }


    }
}
package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CarePlanServiceCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @InjectMocks
    private CarePlanServiceCommandService carePlanServiceCommandService;

    @Nested
    @DisplayName("Care Plan 서비스 선택")
    class selectCarePlanService {
        @Test
        @DisplayName("성공")
        void selectCarePlanService_success() {
            CarePlanServiceSelectCommand carePlanServiceSelectCommand = new CarePlanServiceSelectCommand(
                    patientId,
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

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceCommandRepository.existsByCarePlanIdAndProvideServiceId(
                    carePlanId,
                    provideServiceId,
                    patientId
            )).willReturn(false);
            given(carePlanServiceCommandRepository.save(any(CarePlanService.class))).willAnswer(invocation -> invocation.getArgument(0));

            CarePlanServiceSelectResult carePlanServiceSelectResult = carePlanServiceCommandService.selectCarePlanService(carePlanServiceSelectCommand);

            assertThat(carePlanServiceSelectResult.provideServiceId()).isEqualTo(provideServiceId);

            verify(carePlanServiceCommandRepository).save(any(CarePlanService.class));
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void selectCarePlanService_carePlanNotFound() {
            CarePlanServiceSelectCommand command = new CarePlanServiceSelectCommand(
                    patientId,
                    carePlanId,
                    provideServiceId
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceCommandService.selectCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceCommandRepository, never()).save(any(CarePlanService.class));
        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void selectCarePlanService_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            CarePlanServiceSelectCommand carePlanServiceSelectCommand = new CarePlanServiceSelectCommand(
                    patientId,
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

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> carePlanServiceCommandService.selectCarePlanService(carePlanServiceSelectCommand))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceCommandRepository, never()).save(any(CarePlanService.class));
        }

        @Test
        @DisplayName("환자가 이미 선택한 서비스이면 예외")
        void selectCarePlanService_alreadyExists() {
            CarePlanServiceSelectCommand carePlanServiceSelectCommand = new CarePlanServiceSelectCommand(
                    patientId,
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

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceCommandRepository.existsByCarePlanIdAndProvideServiceId(
                    carePlanId,
                    provideServiceId,
                    patientId
            )).willReturn(true);

            assertThatThrownBy(() -> carePlanServiceCommandService.selectCarePlanService(carePlanServiceSelectCommand))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceCommandRepository, never()).save(any(CarePlanService.class));
        }
    }
}
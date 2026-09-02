package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CarePlanCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();
    UUID dischargePatientId = UUID.randomUUID();

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @InjectMocks
    private CarePlanCommandService carePlanCommandService;

    @Nested
    @DisplayName("Care Plan 생성")
    class CreateCarePlan {
        @Test
        @DisplayName("실제 퇴원일 다음날부터 30일의 Care Plan 생성")
        void createCarePlan_success() {
            LocalDate actualDate = LocalDate.of(2026, 8, 1);

            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    "Care Plan 생성",
                    List.of(provideServiceId)
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    actualDate
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(false);
            given(carePlanCommandRepository.save(any(CarePlan.class))).willAnswer(invocation -> invocation.getArgument(0));

            carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            );

            ArgumentCaptor<CarePlan> captor = ArgumentCaptor.forClass(CarePlan.class);

            verify(carePlanCommandRepository).save(captor.capture());

            CarePlan savedCarePlan = captor.getValue();

            assertThat(savedCarePlan.getPatientId()).isEqualTo(patientId);
            assertThat(savedCarePlan.getDischargeId()).isEqualTo(dischargeId);
            assertThat(savedCarePlan.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 2));
            assertThat(savedCarePlan.getFinishDate()).isEqualTo(LocalDate.of(2026, 8, 31));

            verify(carePlanServiceCommandRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("서비스를 선택하지 않아도 Care Plan 생성 가능")
        void createCarePlan_noService_success() {
            LocalDate actualDate = LocalDate.of(2026, 8, 1);

            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    null,
                    null
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    actualDate
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(false);
            given(carePlanCommandRepository.save(any(CarePlan.class))).willAnswer(invocation -> invocation.getArgument(0));

            carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            );

            verify(carePlanCommandRepository).save(any(CarePlan.class));
            verify(carePlanServiceCommandRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("동일한 퇴원 건으로 생성 시 예외")
        void createCarePlan_duplicate() {
            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    null,
                    null
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    LocalDate.of(2026, 8, 1)
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(true);

            assertThatThrownBy(() -> carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            )).isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).save(any(CarePlan.class));
            verify(carePlanServiceCommandRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("퇴원 건의 환자와 요청 환자가 다르면 예외")
        void createCarePlan_patientMismatch() {
            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    null,
                    null
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    dischargePatientId,
                    LocalDate.of(2026, 8, 1)
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(false);

            assertThatThrownBy(() -> carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            )).isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).save(any(CarePlan.class));
            verify(carePlanServiceCommandRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("실제 퇴원일이 없으면 Care Plan 예외")
        void createCarePlan_actualDateNull() {
            CarePlanCreateCommand command = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    null,
                    null
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    null
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(false);

            assertThatThrownBy(() -> carePlanCommandService.createCarePlan(
                    command,
                    dischargeFindResult
            )).isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).save(any(CarePlan.class));
            verify(carePlanServiceCommandRepository, never()).saveAll(anyList());
        }
    }
}
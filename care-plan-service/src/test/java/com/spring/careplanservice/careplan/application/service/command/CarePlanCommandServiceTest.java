package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.CarePlanCreateCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanDeleteCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanStatusUpdateCommand;
import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.CarePlanCreateResult;
import com.spring.careplanservice.careplan.application.result.CarePlanStatusUpdateResult;
import com.spring.careplanservice.careplan.application.result.DischargeFindResult;
import com.spring.careplanservice.careplan.application.result.UserFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CarePlanCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();
    UUID dischargePatientId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();
    UUID regionId = UUID.randomUUID();

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @InjectMocks
    private CarePlanCommandService carePlanCommandService;

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Mock
    private ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

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
                    List.of(provideServiceId),
                    userId,
                    UserRole.HOSPITAL_STAFF
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
                    null,
                    userId,
                    UserRole.HOSPITAL_STAFF
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
                    null,
                    userId,
                    UserRole.HOSPITAL_STAFF
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
                    null,
                    userId,
                    UserRole.HOSPITAL_STAFF
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
            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    null,
                    null,
                    userId,
                    UserRole.HOSPITAL_STAFF
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    null
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
        @DisplayName("환자는 본인의 Care Plan 생성")
        void createCarePlan_patientOwnCarePlan_success() {
            LocalDate actualDate = LocalDate.of(2026, 9, 1);

            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    patientId,
                    dischargeId,
                    "Care Plan 생성",
                    List.of(),
                    patientId,
                    UserRole.PATIENT
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    patientId,
                    actualDate
            );

            given(carePlanCommandRepository.existsByDischargeId(dischargeId)).willReturn(false);
            given(carePlanCommandRepository.save(any(CarePlan.class))).willAnswer(invocation -> invocation.getArgument(0));

            CarePlanCreateResult result = carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            );

            assertThat(result).isNotNull();

            verify(carePlanCommandRepository).save(any(CarePlan.class));
        }

        @Test
        @DisplayName("환자가 다른 환자의 Care Plan을 생성하면 예외")
        void createCarePlan_otherPatient_throwsException() {
            UUID otherPatientId = UUID.randomUUID();
            CarePlanCreateCommand carePlanCreateCommand = new CarePlanCreateCommand(
                    otherPatientId,
                    dischargeId,
                    "Care Plan 생성",
                    List.of(),
                    userId,
                    UserRole.PATIENT
            );

            DischargeFindResult dischargeFindResult = new DischargeFindResult(
                    dischargeId,
                    otherPatientId,
                    LocalDate.of(2026, 9, 1)
            );

            assertThatThrownBy(() -> carePlanCommandService.createCarePlan(
                    carePlanCreateCommand,
                    dischargeFindResult
            )).isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).save(any(CarePlan.class));
        }
    }

    @Nested
    @DisplayName("Care Plan 완료 처리")
    class CompleteCarePlan {
        @Test
        @DisplayName("성공")
        void completeCarePlan_success() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );

            carePlan.updateStatus(CarePlanStatus.IN_PROGRESS);

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            carePlanCommandService.completeCarePlan(carePlanId);

            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.COMPLETED);
            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void completeCarePlan_carePlanNotFound() {
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanCommandService.completeCarePlan(carePlanId))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태이면 중복 처리하지 않는다")
        void completeCarePlan_alreadyCompleted() {
            CarePlan carePlan = spy(CarePlan.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            ));

            carePlan.updateStatus(CarePlanStatus.IN_PROGRESS);
            carePlan.complete();

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            carePlanCommandService.completeCarePlan(carePlanId);

            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.COMPLETED);
            verify(carePlanCommandRepository).findById(carePlanId);
        }
    }

    @Nested
    @DisplayName("Care plan 상태 수정")
    class CarePlan_update {
        @Test
        @DisplayName("UNDER_REVIEW 상태는 CONFIRMED 상태로 변경 가능")
        void updateCarePlanStatus_underReviewToConfirmed_success() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );

            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.PATIENT,
                    carePlanId,
                    CarePlanStatus.CONFIRMED
            );
            given(userQueryPort.findById(patientId)).willReturn(new UserFindResult(
                    patientId,
                    UserRole.PATIENT,
                    regionId
            ));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            CarePlanStatusUpdateResult carePlanStatusUpdateResult = carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand);

            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.CONFIRMED);
            assertThat(carePlanStatusUpdateResult.status()).isEqualTo(CarePlanStatus.CONFIRMED);

            verify(carePlanCommandRepository).findById(carePlanId);
            verify(userQueryPort).findById(patientId);
            verify(applicationEventPublisher).publishEvent(any(CarePlanConfirmedEvent.class));
        }

        @Test
        @DisplayName("CONFIRMED 상태는 IN_PROGRESS 상태로 변경 가능")
        void updateCarePlanStatus_confirmedToInProgress_success() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );
            carePlan.updateStatus(
                    CarePlanStatus.CONFIRMED
            );

            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.SERVICE_PROVIDER,
                    carePlanId,
                    CarePlanStatus.IN_PROGRESS
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            CarePlanStatusUpdateResult carePlanStatusUpdateResult = carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand);

            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.IN_PROGRESS);
            assertThat(carePlanStatusUpdateResult.status()).isEqualTo(CarePlanStatus.IN_PROGRESS);

            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("UNDER_REVIEW 상태에서 IN_PROGRESS로 변경하면 예외")
        void updateCarePlanStatus_underReviewToInProgress_throwsException() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );

            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.SERVICE_PROVIDER,
                    carePlanId,
                    CarePlanStatus.IN_PROGRESS
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand))
                    .isInstanceOf(BusinessException.class);
            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.UNDER_REVIEW);

            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("CONFIRMED 상태에서 COMPLETED로 변경하면 예외")
        void updateCarePlanStatus_confirmedToCompleted_throwsException() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );

            carePlan.updateStatus(
                    CarePlanStatus.CONFIRMED
            );

            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.SERVICE_PROVIDER,
                    carePlanId,
                    CarePlanStatus.COMPLETED
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand))
                    .isInstanceOf(BusinessException.class);
            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.CONFIRMED);
            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태는 API를 통해 COMPLETED로 변경할 수 없음")
        void updateCarePlanStatus_inProgressToCompleted_throwsException() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );

            carePlan.updateStatus(
                    CarePlanStatus.IN_PROGRESS
            );

            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.SERVICE_PROVIDER,
                    carePlanId,
                    CarePlanStatus.COMPLETED
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            assertThatThrownBy(() -> carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand)).isInstanceOf(BusinessException.class);
            assertThat(carePlan.getStatus()).isEqualTo(CarePlanStatus.IN_PROGRESS);
            verify(carePlanCommandRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void updateCarePlanStatus_carePlanNotFound() {
            CarePlanStatusUpdateCommand carePlanStatusUpdateCommand = new CarePlanStatusUpdateCommand(
                    userId,
                    UserRole.SERVICE_PROVIDER,
                    carePlanId,
                    CarePlanStatus.CONFIRMED
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanCommandService.updateCarePlanStatus(carePlanStatusUpdateCommand))
                    .isInstanceOf(BusinessException.class);
            verify(carePlanCommandRepository).findById(carePlanId);
        }
    }

    @Nested
    @DisplayName("Care Plan 삭제")
    class DeleteCarePlan {
        @Test
        @DisplayName("성공 - 희망 일정 -> 서비스 -> Care Plan 순으로 논리삭제")
        void deleteCarePlan_success() {
            CarePlan carePlan = spy(CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            ));

            CarePlanService carePlanService = spy(CarePlanService.create(
                    carePlanId,
                    provideServiceId
            ));

            CarePlanServicePreference preference = spy(CarePlanServicePreference.create(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            ));

            CarePlanDeleteCommand carePlanDeleteCommand = new CarePlanDeleteCommand(
                    userId,
                    carePlanId
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceCommandRepository.findAllByCarePlanId(carePlan.getId())).willReturn(List.of(carePlanService));
            given(servicePreferenceCommandRepository.findAllByPlanServiceIds(anyList())).willReturn(List.of(preference));

            carePlanCommandService.deleteCarePlan(carePlanDeleteCommand);

            InOrder inOrder = inOrder(preference, carePlanService, carePlan);
            inOrder.verify(preference).delete(userId);
            inOrder.verify(carePlanService).delete(userId);
            inOrder.verify(carePlan).delete(userId);

            assertThat(carePlan.getDeletedAt()).isNotNull();
            assertThat(carePlan.getDeletedBy()).isEqualTo(userId);
            assertThat(carePlanService.getDeletedAt()).isNotNull();
            assertThat(preference.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void deleteCarePlan_carePlanNotFound() {
            CarePlanDeleteCommand carePlanDeleteCommand = new CarePlanDeleteCommand(
                    userId,
                    carePlanId
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanCommandService.deleteCarePlan(carePlanDeleteCommand))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceCommandRepository, never()).findAllByCarePlanId(any(UUID.class));
        }

        @Test
        @DisplayName("UNDER_REVIEW 상태가 아니면 예외")
        void deleteCarePlan_notUnderReview() {
            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 30),
                    null
            );
            carePlan.updateStatus(CarePlanStatus.CONFIRMED);

            CarePlanDeleteCommand carePlanDeleteCommand = new CarePlanDeleteCommand(
                    userId,
                    carePlanId
            );

            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> carePlanCommandService.deleteCarePlan(carePlanDeleteCommand))
                    .isInstanceOf(BusinessException.class);

            assertThat(carePlan.getDeletedAt()).isNull();
            verify(carePlanServiceCommandRepository, never()).findAllByCarePlanId(any(UUID.class));
        }
    }
}
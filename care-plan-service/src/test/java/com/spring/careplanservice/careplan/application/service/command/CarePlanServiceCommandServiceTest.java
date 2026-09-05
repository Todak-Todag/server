package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.application.command.CarePlanServiceCancelCommand;
import com.spring.careplanservice.careplan.application.command.CarePlanServiceSelectCommand;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSelectResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanServiceCommandRepository;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CarePlanServiceCommandServiceTest {
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();
    UUID carePlanId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID planServiceId = UUID.randomUUID();

    @Mock
    private CarePlanCommandRepository carePlanCommandRepository;

    @Mock
    private CarePlanServiceCommandRepository carePlanServiceCommandRepository;

    @Mock
    private ServicePreferenceCommandRepository servicePreferenceCommandRepository;

    @Mock
    private CarePlanOwnerValidator carePlanOwnerValidator;

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
            given(carePlanServiceCommandRepository.existsByCarePlanIdAndProvideServiceIdAndCreatedBy(
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

            CarePlanServiceSelectCommand command = new CarePlanServiceSelectCommand(
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

            doThrow(new BusinessException(ErrorCode.AUTH_FORBIDDEN))
                    .when(carePlanOwnerValidator)
                    .validate(patientId, otherPatientId);

            assertThatThrownBy(() -> carePlanServiceCommandService.selectCarePlanService(command)).isInstanceOf(BusinessException.class);
            verify(carePlanOwnerValidator).validate(patientId, otherPatientId);
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
            given(carePlanServiceCommandRepository.existsByCarePlanIdAndProvideServiceIdAndCreatedBy(
                    carePlanId,
                    provideServiceId,
                    patientId
            )).willReturn(true);

            assertThatThrownBy(() -> carePlanServiceCommandService.selectCarePlanService(carePlanServiceSelectCommand))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceCommandRepository, never()).save(any(CarePlanService.class));
        }
    }

    @Nested
    @DisplayName("Care Plan 서비스 신청 취소")
    class cancelCarePlanService {
        @Test
        @DisplayName("성공 - 희망 일정 -> 서비스 순으로 논리삭제")
        void cancelCarePlanService_success() {
            CarePlanService carePlanService = spy(CarePlanService.create(
                    carePlanId,
                    provideServiceId
            ));

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanServicePreference preference = spy(CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            ));

            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceCommandRepository.findAllByPlanServiceIds(anyList())).willReturn(List.of(preference));

            carePlanServiceCommandService.cancelCarePlanService(command);

            // InOrder : 메서드 호출 순서까지 검증
            InOrder inOrder = inOrder(preference, carePlanService);
            inOrder.verify(preference).delete(patientId);
            inOrder.verify(carePlanService).delete(patientId);

            assertThat(carePlanService.getDeletedAt()).isNotNull();
            assertThat(preference.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 planServiceId면 예외")
        void cancelCarePlanService_notFound() {
            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceCommandService.cancelCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("이미 취소된 서비스 항목이면 예외")
        void cancelCarePlanService_alreadyDeleted() {
            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );
            carePlanService.delete(patientId);

            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));

            assertThatThrownBy(() -> carePlanServiceCommandService.cancelCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanCommandRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void cancelCarePlanService_carePlanNotFound() {
            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceCommandService.cancelCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceCommandRepository, never()).findAllByPlanServiceIds(anyList());
        }

        @Test
        @DisplayName("요청자가 Care Plan의 환자가 아니면 예외")
        void cancelCarePlanService_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

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

            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            doThrow(new BusinessException(ErrorCode.AUTH_FORBIDDEN))
                    .when(carePlanOwnerValidator)
                    .validate(patientId, otherPatientId);

            assertThatThrownBy(() -> carePlanServiceCommandService.cancelCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceCommandRepository, never()).findAllByPlanServiceIds(anyList());
        }

        @Test
        @DisplayName("Care Plan이 UNDER_REVIEW가 아니면 예외")
        void cancelCarePlanService_notUnderReview() {
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
            carePlan.updateStatus(CarePlanStatus.CONFIRMED);

            CarePlanServiceCancelCommand command = new CarePlanServiceCancelCommand(
                    patientId,
                    planServiceId
            );

            given(carePlanServiceCommandRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanCommandRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            assertThatThrownBy(() -> carePlanServiceCommandService.cancelCarePlanService(command))
                    .isInstanceOf(BusinessException.class);

            assertThat(carePlanService.getDeletedAt()).isNull();
            verify(servicePreferenceCommandRepository, never()).findAllByPlanServiceIds(anyList());
        }
    }
}
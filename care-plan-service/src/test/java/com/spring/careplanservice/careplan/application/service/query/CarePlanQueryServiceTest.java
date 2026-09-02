package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindByPreferenceQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanFindQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPreferenceResult;
import com.spring.careplanservice.careplan.application.result.CarePlanFindResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class CarePlanQueryServiceTest {
    UUID carePlanId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID servicePreferenceId = UUID.randomUUID();
    UUID planServiceId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();

    @Mock
    private CarePlanQueryRepository carePlanQueryRepository;

    @InjectMocks
    private CarePlanQueryService carePlanQueryService;

    @Mock
    private ServicePreferenceQueryRepository servicePreferenceQueryRepository;

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Nested
    @DisplayName("patientId 기반 Care Plan 조회")
    class FindByPatient {
        @Test
        @DisplayName("patientId로 Care Plan 조회")
        void findByPatient_success() {
            CarePlan carePlan = Mockito.mock(CarePlan.class);

            given(carePlan.getId()).willReturn(carePlanId);
            given(carePlan.getPatientId()).willReturn(patientId);
            given(carePlan.getStatus()).willReturn(CarePlanStatus.CONFIRMED);

            given(carePlanQueryRepository.findByPatientIdAndStatuses(
                    patientId,
                    Set.of(
                            CarePlanStatus.CONFIRMED,
                            CarePlanStatus.IN_PROGRESS,
                            CarePlanStatus.COMPLETED
                    ))).willReturn(Optional.of(carePlan));

            CarePlanFindByPatientQuery carePlanFindByPatientQuery = new CarePlanFindByPatientQuery(patientId);
            CarePlanFindByPatientResult carePlanFindByPatientResult = carePlanQueryService.findByPatient(carePlanFindByPatientQuery);

            assertThat(carePlanFindByPatientResult.carePlanId()).isEqualTo(carePlanId);
            assertThat(carePlanFindByPatientResult.patientId()).isEqualTo(patientId);
            assertThat(carePlanFindByPatientResult.status()).isEqualTo(CarePlanStatus.CONFIRMED);
        }

        @Test
        @DisplayName("patientId에 해당하는 Care Plan이 없으면 예외 발생")
        void findByPatient_notFound() {
            given(carePlanQueryRepository.findByPatientIdAndStatuses(
                    patientId,
                    Set.of(
                            CarePlanStatus.CONFIRMED,
                            CarePlanStatus.IN_PROGRESS,
                            CarePlanStatus.COMPLETED
                    )
            )).willReturn(Optional.empty());

            CarePlanFindByPatientQuery carePlanFindByPatientQuery = new CarePlanFindByPatientQuery(patientId);

            assertThatThrownBy(() -> carePlanQueryService.findByPatient(carePlanFindByPatientQuery))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("servicePreferenceId 기반 Care Plan 조회")
    class FindByServicePreference {
        @Test
        @DisplayName("servicePreferenceId로 Care Plan 조회")
        void findByServicePreference_success() {
            LocalDate finishDate = LocalDate.now().plusDays(7);

            CarePlanServicePreference preference = Mockito.mock(CarePlanServicePreference.class);
            CarePlanService carePlanService = Mockito.mock(CarePlanService.class);
            CarePlan carePlan = Mockito.mock(CarePlan.class);

            given(preference.getPlanServiceId()).willReturn(planServiceId);
            given(carePlanService.getCarePlanId()).willReturn(carePlanId);
            given(carePlan.getId()).willReturn(carePlanId);
            given(carePlan.getFinishDate()).willReturn(finishDate);
            given(carePlan.getPatientId()).willReturn(patientId);
            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            CarePlanFindByPreferenceQuery carePlanFindByPreferenceQuery = new CarePlanFindByPreferenceQuery(servicePreferenceId);

            CarePlanFindByPreferenceResult carePlanFindByPreferenceResult = carePlanQueryService.findByServicePreference(carePlanFindByPreferenceQuery);

            assertThat(carePlanFindByPreferenceResult.carePlanId()).isEqualTo(carePlanId);
            assertThat(carePlanFindByPreferenceResult.patientId()).isEqualTo(patientId);
            assertThat(carePlanFindByPreferenceResult.finishDate()).isEqualTo(finishDate);
        }

        @Test
        @DisplayName("존재하지 않는 servicePreferenceId면 예외 발생")
        void findByServicePreference_notFound() {
            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.empty());

            CarePlanFindByPreferenceQuery carePlanFindByPreferenceQuery = new CarePlanFindByPreferenceQuery(servicePreferenceId);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> carePlanQueryService.findByServicePreference(carePlanFindByPreferenceQuery)
            );

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Care Plan 단건 조회")
    class FindCarePlan {
        @Test
        @DisplayName("성공")
        void findCarePlan_success() {
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate finishDate = LocalDate.of(2026, 9, 30);

            CarePlan carePlan = Mockito.mock(CarePlan.class);

            given(carePlan.getId()).willReturn(carePlanId);
            given(carePlan.getPatientId()).willReturn(patientId);
            given(carePlan.getDischargeId()).willReturn(dischargeId);
            given(carePlan.getStatus()).willReturn(CarePlanStatus.UNDER_REVIEW);
            given(carePlan.getStartDate()).willReturn(startDate);
            given(carePlan.getFinishDate()).willReturn(finishDate);
            given(carePlan.getNote()).willReturn("퇴원 후 방문 간호가 필요합니다.");

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            CarePlanFindQuery carePlanFindQuery = new CarePlanFindQuery(
                    carePlanId,
                    patientId
            );

            CarePlanFindResult carePlanFindResult = carePlanQueryService.findCarePlan(carePlanFindQuery);

            assertThat(carePlanFindResult.carePlanId()).isEqualTo(carePlanId);
            assertThat(carePlanFindResult.patientId()).isEqualTo(patientId);
            assertThat(carePlanFindResult.dischargeId()).isEqualTo(dischargeId);
            assertThat(carePlanFindResult.status()).isEqualTo(CarePlanStatus.UNDER_REVIEW);
            assertThat(carePlanFindResult.startDate()).isEqualTo(startDate);
            assertThat(carePlanFindResult.finishDate()).isEqualTo(finishDate);
            assertThat(carePlanFindResult.note()).isEqualTo("퇴원 후 방문 간호가 필요합니다.");

            verify(carePlanQueryRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void findCarePlan_notFound() {
            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.empty());

            CarePlanFindQuery carePlanFindQuery = new CarePlanFindQuery(
                    carePlanId,
                    patientId
            );

            BusinessException exception = assertThrows(BusinessException.class, () -> carePlanQueryService.findCarePlan(carePlanFindQuery));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND);

            verify(carePlanQueryRepository).findById(carePlanId);
        }

        @Test
        @DisplayName("요청자가 Care Plan 소유자가 아니면 예외")
        void findCarePlan_forbidden() {

        }
    }
}
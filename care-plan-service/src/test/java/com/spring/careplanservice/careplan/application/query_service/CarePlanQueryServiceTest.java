package com.spring.careplanservice.careplan.application.query_service;

import com.spring.careplanservice.careplan.application.query.CarePlanFindByPatientQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanFindByPatientResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class CarePlanQueryServiceTest {
    UUID carePlanId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    @Mock
    private CarePlanQueryRepository carePlanQueryRepository;

    @InjectMocks
    private CarePlanQueryService carePlanQueryService;

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
        @DisplayName("patientId에 해당하는 Care Plan이 없으면 예외가 발생한다")
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

    }

}
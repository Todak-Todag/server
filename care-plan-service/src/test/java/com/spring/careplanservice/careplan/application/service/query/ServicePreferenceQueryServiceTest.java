package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.query.ServicePreferenceSearchQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicePreferenceQueryServiceTest {
    UUID carePlanId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID servicePreferenceId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();

    @Mock
    private CarePlanQueryRepository carePlanQueryRepository;

    @Mock
    private ServicePreferenceQueryRepository servicePreferenceQueryRepository;

    @Mock
    private CarePlanOwnerValidator carePlanOwnerValidator;

    @InjectMocks
    private ServicePreferenceQueryService servicePreferenceQueryService;

    @Nested
    @DisplayName("서비스 희망 일정 목록 조회")
    class SearchServicePreferences {
        @Test
        @DisplayName("PATIENT 본인 소유 Care Plan이면 성공")
        void searchServicePreferences_patientOwner_success() {
            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    null,
                    0,
                    10
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            ServicePreferenceSearchResult result = new ServicePreferenceSearchResult(
                    servicePreferenceId,
                    provideServiceId,
                    LocalDate.of(2026, 9, 10),
                    null,
                    Instant.parse("2026-08-28T03:30:00Z")
            );

            Page<ServicePreferenceSearchResult> page = new PageImpl<>(
                    List.of(result),
                    PageRequest.of(0, 10),
                    1
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(page);

            Page<ServicePreferenceSearchResult> resultPage = servicePreferenceQueryService.searchServicePreferences(query);

            assertThat(resultPage.getContent()).containsExactly(result);
            verify(carePlanOwnerValidator).validate(patientId, patientId);
        }

        @Test
        @DisplayName("HOSPITAL_STAFF는 소유권 검증 없이 성공")
        void searchServicePreferences_hospitalStaff_skipsOwnerCheck() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    UUID.randomUUID(),
                    UserRole.HOSPITAL_STAFF,
                    carePlanId,
                    null,
                    0,
                    10
            );

            CarePlan carePlan = CarePlan.create(
                    otherPatientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(Page.empty());

            servicePreferenceQueryService.searchServicePreferences(query);

            verify(carePlanOwnerValidator, never()).validate(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("SOCIAL_WORKER는 소유권 검증 없이 성공")
        void searchServicePreferences_socialWorker_skipsOwnerCheck() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    UUID.randomUUID(),
                    UserRole.SOCIAL_WORKER,
                    carePlanId,
                    null,
                    0,
                    10
            );

            CarePlan carePlan = CarePlan.create(
                    otherPatientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(Page.empty());

            servicePreferenceQueryService.searchServicePreferences(query);

            verify(carePlanOwnerValidator, never()).validate(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void searchServicePreferences_carePlanNotFound() {
            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    null,
                    0,
                    10
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceQueryService.searchServicePreferences(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND)
                    );

            verify(servicePreferenceQueryRepository, never()).search(any(UUID.class), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("PATIENT가 본인 소유가 아니면 예외")
        void searchServicePreferences_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    null,
                    0,
                    10
            );

            CarePlan carePlan = CarePlan.create(
                    otherPatientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            doThrow(new BusinessException(ErrorCode.AUTH_FORBIDDEN))
                    .when(carePlanOwnerValidator)
                    .validate(patientId, otherPatientId);

            assertThatThrownBy(() -> servicePreferenceQueryService.searchServicePreferences(query))
                    .isInstanceOf(BusinessException.class);

            verify(servicePreferenceQueryRepository, never()).search(any(UUID.class), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("허용되지 않은 페이지 크기는 10 고정")
        void searchServicePreferences_invalidSize_defaultsTo10() {
            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    null,
                    0,
                    20
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(Page.empty());

            servicePreferenceQueryService.searchServicePreferences(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(servicePreferenceQueryRepository).search(eq(carePlanId), eq((LocalDate) null), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("page가 음수여도 예외 없이 0으로 보정")
        void searchServicePreferences_negativePage_resolvesToZero() {
            ServicePreferenceSearchQuery query = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    null,
                    -1,
                    10
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(Page.empty());

            servicePreferenceQueryService.searchServicePreferences(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(servicePreferenceQueryRepository).search(eq(carePlanId), eq((LocalDate) null), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        }
    }
}

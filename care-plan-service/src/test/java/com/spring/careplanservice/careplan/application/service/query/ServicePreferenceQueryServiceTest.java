package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.query.ServicePreferenceFindQuery;
import com.spring.careplanservice.careplan.application.query.ServicePreferenceSearchQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceFindResult;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceView;
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
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

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

            ServicePreferenceView view = new ServicePreferenceView(
                    servicePreferenceId,
                    provideServiceId,
                    LocalDate.of(2026, 9, 10),
                    null,
                    Instant.parse("2026-08-28T03:30:00Z")
            );

            Page<ServicePreferenceView> page = new PageImpl<>(
                    List.of(view),
                    PageRequest.of(0, 10),
                    1
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(page);

            Page<ServicePreferenceSearchResult> resultPage = servicePreferenceQueryService.searchServicePreferences(query);

            assertThat(resultPage.getContent()).hasSize(1);

            ServicePreferenceSearchResult result = resultPage.getContent().getFirst();

            assertThat(result.servicePreferenceId()).isEqualTo(servicePreferenceId);
            assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
            assertThat(result.preferredDate()).isEqualTo(LocalDate.of(2026, 9, 10));
            assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-08-28T03:30:00Z"));
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

        @Test
        @DisplayName("preferredDate가 있으면 조회 조건으로 전달")
        void searchServicePreferences_withPreferredDate_passesFilter() {
            LocalDate preferredDate = LocalDate.of(2026, 9, 10);

            ServicePreferenceSearchQuery servicePreferenceSearchQuery = new ServicePreferenceSearchQuery(
                    patientId,
                    UserRole.PATIENT,
                    carePlanId,
                    preferredDate,
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

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(
                    eq(carePlanId),
                    eq(preferredDate),
                    any(Pageable.class)
            )).willReturn(Page.empty());

            servicePreferenceQueryService.searchServicePreferences(servicePreferenceSearchQuery);

            verify(servicePreferenceQueryRepository).search(
                    eq(carePlanId),
                    eq(preferredDate),
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("조회 View를 SearchResult로 변환")
        void searchServicePreferences_mapsViewToResult() {
            LocalDate preferredDate = LocalDate.of(2026, 9, 10);

            Instant createdAt = Instant.parse("2026-08-28T03:30:00Z");
            ServicePreferenceSearchQuery servicePreferenceSearchQuery = new ServicePreferenceSearchQuery(
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

            ServicePreferenceView view = new ServicePreferenceView(
                    servicePreferenceId,
                    provideServiceId,
                    preferredDate,
                    PreferredTimeSlot.MORNING,
                    createdAt
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(servicePreferenceQueryRepository.search(eq(carePlanId), eq((LocalDate) null), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(view), PageRequest.of(0, 10), 1));

            Page<ServicePreferenceSearchResult> resultPage = servicePreferenceQueryService.searchServicePreferences(servicePreferenceSearchQuery);

            ServicePreferenceSearchResult result = resultPage.getContent().getFirst();

            assertThat(result.servicePreferenceId()).isEqualTo(servicePreferenceId);
            assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
            assertThat(result.preferredDate()).isEqualTo(preferredDate);
            assertThat(result.preferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);
            assertThat(result.createdAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("서비스 희망 일정 단건 조회")
    class FindServicePreference {
        @Test
        @DisplayName("PATIENT 본인 소유이면 성공")
        void findServicePreference_patientOwner_success() {
            ServicePreferenceFindQuery query = new ServicePreferenceFindQuery(
                    patientId,
                    UserRole.PATIENT,
                    servicePreferenceId
            );

            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    UUID.randomUUID(),
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

            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(preference.getPlanServiceId())).willReturn(Optional.of(carePlanService));
            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            ServicePreferenceFindResult result = servicePreferenceQueryService.findServicePreference(query);

            assertThat(result.servicePreferenceId()).isEqualTo(preference.getId());
            assertThat(result.planServiceId()).isEqualTo(preference.getPlanServiceId());
            assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
            assertThat(result.preferredDate()).isEqualTo(LocalDate.of(2026, 9, 10));
            assertThat(result.preferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);
            verify(carePlanOwnerValidator).validate(patientId, patientId);
        }

        @Test
        @DisplayName("존재하지 않는 servicePreferenceId면 예외")
        void findServicePreference_notFound() {
            ServicePreferenceFindQuery query = new ServicePreferenceFindQuery(
                    patientId,
                    UserRole.PATIENT,
                    servicePreferenceId
            );

            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceQueryService.findServicePreference(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.SERVICE_PREFERENCE_NOT_FOUND)
                    );

            verify(carePlanServiceQueryRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("Care Plan 서비스가 존재하지 않으면 예외")
        void findServicePreference_planServiceNotFound() {
            ServicePreferenceFindQuery query = new ServicePreferenceFindQuery(
                    patientId,
                    UserRole.PATIENT,
                    servicePreferenceId
            );

            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(preference.getPlanServiceId())).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceQueryService.findServicePreference(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND)
                    );

            verify(carePlanQueryRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void findServicePreference_carePlanNotFound() {
            ServicePreferenceFindQuery query = new ServicePreferenceFindQuery(
                    patientId,
                    UserRole.PATIENT,
                    servicePreferenceId
            );

            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(preference.getPlanServiceId())).willReturn(Optional.of(carePlanService));
            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> servicePreferenceQueryService.findServicePreference(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND)
                    );

            verify(carePlanOwnerValidator, never()).validate(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("PATIENT가 본인 소유가 아니면 예외")
        void findServicePreference_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            ServicePreferenceFindQuery query = new ServicePreferenceFindQuery(
                    patientId,
                    UserRole.PATIENT,
                    servicePreferenceId
            );

            CarePlanServicePreference preference = CarePlanServicePreference.create(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
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

            given(servicePreferenceQueryRepository.findById(servicePreferenceId)).willReturn(Optional.of(preference));
            given(carePlanServiceQueryRepository.findById(preference.getPlanServiceId())).willReturn(Optional.of(carePlanService));
            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));

            doThrow(new BusinessException(ErrorCode.AUTH_FORBIDDEN))
                    .when(carePlanOwnerValidator)
                    .validate(patientId, otherPatientId);

            assertThatThrownBy(() -> servicePreferenceQueryService.findServicePreference(query))
                    .isInstanceOf(BusinessException.class);
        }
    }
}

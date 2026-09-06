package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.port.ProviderServiceQueryPort;
import com.spring.careplanservice.careplan.application.query.CarePlanServiceFindQuery;
import com.spring.careplanservice.careplan.application.query.CarePlanServiceSearchQuery;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceFindResult;
import com.spring.careplanservice.careplan.application.result.CarePlanServiceSearchResult;
import com.spring.careplanservice.careplan.application.result.ProvideServiceInfoResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.entity.PreferredTimeSlot;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

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
class CarePlanServiceQueryServiceTest {
    UUID carePlanId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();
    UUID dischargeId = UUID.randomUUID();
    UUID provideServiceId = UUID.randomUUID();

    @Mock
    private CarePlanQueryRepository carePlanQueryRepository;

    @Mock
    private CarePlanServiceQueryRepository carePlanServiceQueryRepository;

    @Mock
    private CarePlanOwnerValidator carePlanOwnerValidator;

    @Mock
    private ProviderServiceQueryPort providerServiceQueryPort;

    @Mock
    private ServicePreferenceQueryRepository servicePreferenceQueryRepository;

    @InjectMocks
    private CarePlanServiceQueryService carePlanServiceQueryService;

    @Nested
    @DisplayName("Care Plan 신청 서비스 목록 조회")
    class SearchCarePlanServices {
        @Test
        @DisplayName("PATIENT 본인 소유 Care Plan이면 성공하고 Provider-Service 이름이 채워진다")
        void searchCarePlanServices_patientOwner_success() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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

            CarePlanService carePlanService = CarePlanService.create(
                    carePlanId,
                    provideServiceId
            );

            Page<CarePlanService> page = new PageImpl<>(
                    List.of(carePlanService),
                    PageRequest.of(0, 10),
                    1
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(page);
            given(providerServiceQueryPort.findAllByIds(List.of(provideServiceId)))
                    .willReturn(List.of(new ProvideServiceInfoResult(provideServiceId, "방문 간호", "설명")));

            Page<CarePlanServiceSearchResult> resultPage = carePlanServiceQueryService.searchCarePlanServices(query);

            assertThat(resultPage.getContent()).hasSize(1);

            CarePlanServiceSearchResult result = resultPage.getContent().getFirst();

            assertThat(result.planServiceId()).isEqualTo(carePlanService.getId());
            assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
            assertThat(result.provideServiceName()).isEqualTo("방문 간호");
            assertThat(result.createdAt()).isEqualTo(carePlanService.getCreatedAt());
            verify(carePlanOwnerValidator).validate(patientId, patientId);
        }

        @Test
        @DisplayName("조회 결과가 비어있으면 Provider-Service를 호출하지 않는다")
        void searchCarePlanServices_empty_skipsProviderServiceCall() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(Page.empty());

            Page<CarePlanServiceSearchResult> resultPage = carePlanServiceQueryService.searchCarePlanServices(query);

            assertThat(resultPage.getContent()).isEmpty();
            verify(providerServiceQueryPort, never()).findAllByIds(any());
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void searchCarePlanServices_carePlanNotFound() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
                    0,
                    10
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceQueryService.searchCarePlanServices(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND)
                    );

            verify(carePlanServiceQueryRepository, never()).search(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("PATIENT가 본인 소유가 아니면 예외")
        void searchCarePlanServices_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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

            assertThatThrownBy(() -> carePlanServiceQueryService.searchCarePlanServices(query))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceQueryRepository, never()).search(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("허용되지 않은 페이지 크기는 10 고정")
        void searchCarePlanServices_invalidSize_defaultsTo10() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(Page.empty());

            carePlanServiceQueryService.searchCarePlanServices(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(carePlanServiceQueryRepository).search(eq(carePlanId), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("page가 음수여도 예외 없이 0으로 보정되고 createdAt 내림차순으로 정렬한다")
        void searchCarePlanServices_negativePage_resolvesToZeroAndSortsDescending() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(Page.empty());

            carePlanServiceQueryService.searchCarePlanServices(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(carePlanServiceQueryRepository).search(eq(carePlanId), pageableCaptor.capture());

            Pageable capturedPageable = pageableCaptor.getValue();
            assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
            assertThat(capturedPageable.getSort().getOrderFor("createdAt"))
                    .extracting(Sort.Order::getDirection)
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("동일한 provideServiceId가 중복되어도 Provider-Service 호출 전에 distinct 처리된다")
        void searchCarePlanServices_duplicateProvideServiceIds_callsProviderServiceOnceWithDistinctIds() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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

            CarePlanService carePlanService1 = CarePlanService.create(carePlanId, provideServiceId);
            CarePlanService carePlanService2 = CarePlanService.create(carePlanId, provideServiceId);

            Page<CarePlanService> page = new PageImpl<>(
                    List.of(carePlanService1, carePlanService2),
                    PageRequest.of(0, 10),
                    2
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(page);
            given(providerServiceQueryPort.findAllByIds(List.of(provideServiceId)))
                    .willReturn(List.of(new ProvideServiceInfoResult(provideServiceId, "방문 간호", "설명")));

            carePlanServiceQueryService.searchCarePlanServices(query);

            verify(providerServiceQueryPort).findAllByIds(List.of(provideServiceId));
        }

        @Test
        @DisplayName("Provider-Service 응답에 요청한 provideServiceId가 누락되어 있으면 예외를 던진다")
        void searchCarePlanServices_missingProvideServiceInResponse_throws() {
            CarePlanServiceSearchQuery query = new CarePlanServiceSearchQuery(
                    patientId,
                    carePlanId,
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

            CarePlanService carePlanService = CarePlanService.create(carePlanId, provideServiceId);

            Page<CarePlanService> page = new PageImpl<>(
                    List.of(carePlanService),
                    PageRequest.of(0, 10),
                    1
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.search(eq(carePlanId), any(Pageable.class))).willReturn(page);
            given(providerServiceQueryPort.findAllByIds(List.of(provideServiceId)))
                    .willReturn(List.of());

            assertThatThrownBy(() -> carePlanServiceQueryService.searchCarePlanServices(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.PROVIDER_SERVICE_DATA_MISMATCH)
                    );
        }
    }

    @Nested
    @DisplayName("Care Plan 신청 서비스 단건 조회")
    class FindCarePlanService {
        UUID planServiceId = UUID.randomUUID();

        @Test
        @DisplayName("PATIENT 본인 소유이면 성공하고 preferences는 createdAt 내림차순으로 반환된다")
        void findCarePlanService_patientOwner_success() {
            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanService carePlanService = CarePlanService.create(carePlanId, provideServiceId);
            ReflectionTestUtils.setField(carePlanService, "id", planServiceId);

            CarePlanServicePreference olderPreference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 10),
                    PreferredTimeSlot.MORNING
            );
            ReflectionTestUtils.setField(olderPreference, "createdAt", Instant.parse("2026-08-27T00:00:00Z"));

            CarePlanServicePreference newerPreference = CarePlanServicePreference.create(
                    planServiceId,
                    LocalDate.of(2026, 9, 12),
                    PreferredTimeSlot.AFTERNOON
            );
            ReflectionTestUtils.setField(newerPreference, "createdAt", Instant.parse("2026-08-28T00:00:00Z"));

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(providerServiceQueryPort.findAllByIds(List.of(provideServiceId)))
                    .willReturn(List.of(new ProvideServiceInfoResult(provideServiceId, "방문 간호", "설명")));
            given(servicePreferenceQueryRepository.findAllByPlanServiceIds(List.of(planServiceId)))
                    .willReturn(List.of(olderPreference, newerPreference));

            CarePlanServiceFindResult result = carePlanServiceQueryService.findCarePlanService(query);

            assertThat(result.planServiceId()).isEqualTo(planServiceId);
            assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
            assertThat(result.provideServiceName()).isEqualTo("방문 간호");
            assertThat(result.provideServiceContent()).isEqualTo("설명");
            assertThat(result.preferences()).extracting(CarePlanServiceFindResult.PreferenceSummary::servicePreferenceId)
                    .containsExactly(newerPreference.getId(), olderPreference.getId());
            verify(carePlanOwnerValidator).validate(patientId, patientId);
        }

        @Test
        @DisplayName("Care Plan이 존재하지 않으면 예외")
        void findCarePlanService_carePlanNotFound() {
            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceQueryService.findCarePlanService(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_NOT_FOUND)
                    );

            verify(carePlanServiceQueryRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("PATIENT가 본인 소유가 아니면 예외")
        void findCarePlanService_forbidden() {
            UUID otherPatientId = UUID.randomUUID();

            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
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

            assertThatThrownBy(() -> carePlanServiceQueryService.findCarePlanService(query))
                    .isInstanceOf(BusinessException.class);

            verify(carePlanServiceQueryRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("planServiceId가 존재하지 않으면 예외")
        void findCarePlanService_planServiceNotFound() {
            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> carePlanServiceQueryService.findCarePlanService(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND)
                    );

            verify(providerServiceQueryPort, never()).findAllByIds(any());
        }

        @Test
        @DisplayName("planServiceId가 다른 Care Plan에 속해 있으면 예외")
        void findCarePlanService_planServiceBelongsToDifferentCarePlan() {
            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanService carePlanServiceOfAnotherCarePlan = CarePlanService.create(
                    UUID.randomUUID(),
                    provideServiceId
            );

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.findById(planServiceId))
                    .willReturn(Optional.of(carePlanServiceOfAnotherCarePlan));

            assertThatThrownBy(() -> carePlanServiceQueryService.findCarePlanService(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND)
                    );

            verify(providerServiceQueryPort, never()).findAllByIds(any());
        }

        @Test
        @DisplayName("Provider-Service 응답에 provideServiceId가 없으면 예외")
        void findCarePlanService_providerServiceMismatch_throws() {
            CarePlanServiceFindQuery query = new CarePlanServiceFindQuery(
                    patientId,
                    carePlanId,
                    planServiceId
            );

            CarePlan carePlan = CarePlan.create(
                    patientId,
                    dischargeId,
                    LocalDate.of(2026, 9, 2),
                    LocalDate.of(2026, 10, 1),
                    null
            );

            CarePlanService carePlanService = CarePlanService.create(carePlanId, provideServiceId);
            ReflectionTestUtils.setField(carePlanService, "id", planServiceId);

            given(carePlanQueryRepository.findById(carePlanId)).willReturn(Optional.of(carePlan));
            given(carePlanServiceQueryRepository.findById(planServiceId)).willReturn(Optional.of(carePlanService));
            given(providerServiceQueryPort.findAllByIds(List.of(provideServiceId))).willReturn(List.of());

            assertThatThrownBy(() -> carePlanServiceQueryService.findCarePlanService(query))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception ->
                            assertThat(((BusinessException) exception).getErrorCode())
                                    .isEqualTo(ErrorCode.PROVIDER_SERVICE_DATA_MISMATCH)
                    );

            verify(servicePreferenceQueryRepository, never()).findAllByPlanServiceIds(any());
        }
    }
}

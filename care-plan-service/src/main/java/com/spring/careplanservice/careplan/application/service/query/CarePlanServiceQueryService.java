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
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.global.common.PageableFactory;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarePlanServiceQueryService {
    private final CarePlanQueryRepository carePlanQueryRepository;
    private final CarePlanServiceQueryRepository carePlanServiceQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    private final CarePlanOwnerValidator carePlanOwnerValidator;
    private final ProviderServiceQueryPort providerServiceQueryPort;

    public Page<CarePlanServiceSearchResult> searchCarePlanServices(
            CarePlanServiceSearchQuery carePlanServiceSearchQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository
                .findById(carePlanServiceSearchQuery.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                carePlanServiceSearchQuery.userId(),
                carePlan.getPatientId()
        );

        Pageable pageable = PageableFactory.of(
                carePlanServiceSearchQuery.page(),
                carePlanServiceSearchQuery.size(),
                "createdAt,desc"
        );

        Page<CarePlanService> carePlanServicePage = carePlanServiceQueryRepository.search(
                carePlanServiceSearchQuery.carePlanId(),
                pageable
        );

        Map<UUID, ProvideServiceInfoResult> provideServiceById = findProvideServiceInfos(
                carePlanServicePage.getContent()
        );

        return carePlanServicePage.map(carePlanService ->
                CarePlanServiceSearchResult.of(
                        carePlanService,
                        provideServiceById.get(carePlanService.getProvideServiceId())
                )
        );
    }

    public CarePlanServiceFindResult findCarePlanService(
            CarePlanServiceFindQuery carePlanServiceFindQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository
                .findById(carePlanServiceFindQuery.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        carePlanOwnerValidator.validate(
                carePlanServiceFindQuery.userId(),
                carePlan.getPatientId()
        );

        CarePlanService carePlanService = carePlanServiceQueryRepository
                .findById(carePlanServiceFindQuery.planServiceId())
                .filter(found -> found.getCarePlanId().equals(carePlanServiceFindQuery.carePlanId()))
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_SERVICE_NOT_FOUND
                        )
                );

        Map<UUID, ProvideServiceInfoResult> provideServiceById = findProvideServiceInfos(
                List.of(carePlanService)
        );
        ProvideServiceInfoResult provideServiceInfo = provideServiceById.get(
                carePlanService.getProvideServiceId()
        );

        List<CarePlanServiceFindResult.PreferenceSummary> preferences = servicePreferenceQueryRepository
                .findAllByPlanServiceIds(List.of(carePlanService.getId())).stream()
                .sorted(Comparator.comparing(CarePlanServicePreference::getCreatedAt).reversed())
                .map(preference -> new CarePlanServiceFindResult.PreferenceSummary(
                        preference.getId(),
                        preference.getPreferredDate(),
                        preference.getPreferredTimeSlot()
                ))
                .toList();

        return new CarePlanServiceFindResult(
                carePlanService.getId(),
                carePlanService.getProvideServiceId(),
                provideServiceInfo.name(),
                provideServiceInfo.content(),
                preferences,
                carePlanService.getCreatedAt()
        );
    }

    private Map<UUID, ProvideServiceInfoResult> findProvideServiceInfos(
            List<CarePlanService> carePlanServices
    ) {
        if (carePlanServices.isEmpty()) {
            return Map.of();
        }

        List<UUID> provideServiceIds = carePlanServices.stream()
                .map(CarePlanService::getProvideServiceId)
                .distinct()
                .toList();

        List<ProvideServiceInfoResult> provideServiceInfos = providerServiceQueryPort.findAllByIds(
                provideServiceIds
        );

        validateAllFound(provideServiceIds, provideServiceInfos);

        return provideServiceInfos.stream()
                .collect(Collectors.toMap(
                        ProvideServiceInfoResult::provideServiceId,
                        Function.identity()
                ));
    }

    private void validateAllFound(
            List<UUID> requestedProvideServiceIds,
            List<ProvideServiceInfoResult> provideServiceInfos
    ) {
        Set<UUID> foundProvideServiceIds = provideServiceInfos.stream()
                .map(ProvideServiceInfoResult::provideServiceId)
                .collect(Collectors.toSet());

        boolean hasMissing = requestedProvideServiceIds.stream()
                .anyMatch(provideServiceId -> !foundProvideServiceIds.contains(provideServiceId));

        if (hasMissing) {
            throw new BusinessException(ErrorCode.PROVIDER_SERVICE_DATA_MISMATCH);
        }
    }
}

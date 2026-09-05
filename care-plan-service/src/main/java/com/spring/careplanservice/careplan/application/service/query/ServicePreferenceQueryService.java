package com.spring.careplanservice.careplan.application.service.query;

import com.spring.careplanservice.careplan.application.query.ServicePreferenceSearchQuery;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.application.support.CarePlanOwnerValidator;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceView;
import com.spring.careplanservice.global.common.PageableFactory;
import com.spring.careplanservice.global.common.UserRole;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicePreferenceQueryService {
    private final CarePlanQueryRepository carePlanQueryRepository;
    private final ServicePreferenceQueryRepository servicePreferenceQueryRepository;
    private final CarePlanOwnerValidator carePlanOwnerValidator;

    public Page<ServicePreferenceSearchResult> searchServicePreferences(
            ServicePreferenceSearchQuery servicePreferenceSearchQuery
    ) {
        CarePlan carePlan = carePlanQueryRepository
                .findById(servicePreferenceSearchQuery.carePlanId())
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_NOT_FOUND
                        )
                );

        if (servicePreferenceSearchQuery.role() == UserRole.PATIENT) {
            carePlanOwnerValidator.validate(
                    servicePreferenceSearchQuery.userId(),
                    carePlan.getPatientId()
            );
        }

        Pageable pageable = PageableFactory.of(
                servicePreferenceSearchQuery.page(),
                servicePreferenceSearchQuery.size(),
                null
        );

        return servicePreferenceQueryRepository.search(
                servicePreferenceSearchQuery.carePlanId(),
                servicePreferenceSearchQuery.preferredDate(),
                pageable
        ).map(this::toResult);
    }

    private ServicePreferenceSearchResult toResult(
            ServicePreferenceView view
    ) {
        return new ServicePreferenceSearchResult(
                view.servicePreferenceId(),
                view.provideServiceId(),
                view.preferredDate(),
                view.preferredTimeSlot(),
                view.createdAt()
        );
    }
}

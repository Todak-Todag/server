package com.spring.careplanservice.careplan.application.service.query;


import com.spring.careplanservice.careplan.domain.repository.query.CarePlanOutboxEventQueryRepository;
import com.spring.careplanservice.careplan.application.result.CarePlanOutboxEventResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarePlanOutboxQueryService {
    private final CarePlanOutboxEventQueryRepository carePlanOutboxEventQueryRepository;

    @Transactional(readOnly = true)
    public List<CarePlanOutboxEventResult> findPending(int batchSize) {
        return carePlanOutboxEventQueryRepository
                .findByStatus(
                        CarePlanOutboxEventStatus.PENDING,
                        batchSize
                )
                .stream()
                .map(CarePlanOutboxEventResult::from)
                .toList();
    }
}

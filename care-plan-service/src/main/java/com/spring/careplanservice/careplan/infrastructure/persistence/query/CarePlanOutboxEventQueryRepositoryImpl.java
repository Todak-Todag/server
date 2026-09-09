package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.repository.query.CarePlanOutboxEventQueryRepository;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.SpringDataCarePlanOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CarePlanOutboxEventQueryRepositoryImpl implements CarePlanOutboxEventQueryRepository {
    private final SpringDataCarePlanOutboxEventRepository springDataRepository;

    @Override
    public List<CarePlanOutboxEvent> findByStatus(
            CarePlanOutboxEventStatus status,
            int limit
    ) {
        return springDataRepository.findByStatusOrderByCreatedAtAsc(
                status,
                PageRequest.of(0, limit)
        );
    }
}

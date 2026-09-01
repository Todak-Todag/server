package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceQueryRepositoryImpl implements CarePlanServiceQueryRepository {
    private final JpaCarePlanServiceRepository jpaCarePlanServiceRepository;

    @Override
    public Optional<CarePlanService> findById(UUID id) {
        return jpaCarePlanServiceRepository.findByIdAndDeletedAtIsNull(id);
    }
}

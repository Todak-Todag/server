package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlanService;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanServiceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceQueryRepositoryImpl implements CarePlanServiceQueryRepository {

    @Override
    public Optional<CarePlanService> findById(UUID id) {
        return Optional.empty();
    }
}

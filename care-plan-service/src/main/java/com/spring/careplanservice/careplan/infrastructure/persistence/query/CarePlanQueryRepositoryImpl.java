package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class CarePlanQueryRepositoryImpl implements CarePlanQueryRepository {

    @Override
    public Optional<CarePlan> findById(UUID id) {
        return Optional.empty();
    }
}

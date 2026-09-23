package com.spring.careplanservice.careplan.infrastructure.persistence.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.command.ServicePreferenceCommandRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaServicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServicePreferenceCommandRepositoryImpl implements ServicePreferenceCommandRepository {
    private final JpaServicePreferenceRepository jpaServicePreferenceRepository;

    @Override
    public CarePlanServicePreference save(
            CarePlanServicePreference carePlanServicePreference
    ) {
        return jpaServicePreferenceRepository.save(
                carePlanServicePreference
        );
    }

    @Override
    public Optional<CarePlanServicePreference> findById(UUID id) {
        return jpaServicePreferenceRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<CarePlanServicePreference> findByIdIncludingDeleted(UUID id) {
        return jpaServicePreferenceRepository.findById(id);
    }

    @Override
    public List<CarePlanServicePreference> findAllByPlanServiceIds(
            List<UUID> planServiceIds
    ) {
        return jpaServicePreferenceRepository.findAllByPlanServiceIdInAndDeletedAtIsNull(planServiceIds);
    }
}

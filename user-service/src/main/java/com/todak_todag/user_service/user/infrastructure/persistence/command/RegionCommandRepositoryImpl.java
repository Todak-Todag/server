package com.todak_todag.user_service.user.infrastructure.persistence.command;

import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.command.RegionCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegionCommandRepositoryImpl
        implements RegionCommandRepository {

    private final JpaRegionRepository jpaRepository;

    @Override
    public Region save(Region region) {
        return jpaRepository.save(region);
    }
}
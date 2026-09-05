package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.user.application.command.RegionCreateCommand;
import com.todak_todag.user_service.user.application.result.RegionCreateResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.command.RegionCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegionCommandService {

    private final RegionCommandRepository regionCommandRepository;
    private final RegionQueryRepository regionQueryRepository;

    @Transactional
    public RegionCreateResult createRegion(RegionCreateCommand command) {

        if (regionQueryRepository.existsByRegionCode(command.regionCode())) {
            throw new BusinessException(
                    RegionErrorCode.REGION_DUPLICATE
            );
        }

        Region region = Region.create(
                command.province(),
                command.district(),
                command.regionCode()
        );

        Region savedRegion =
                regionCommandRepository.save(region);

        log.info(
                "[Region] 지역 등록 regionId={} regionCode={}",
                savedRegion.getId(),
                savedRegion.getRegionCode()
        );

        return RegionCreateResult.from(savedRegion);
    }
}
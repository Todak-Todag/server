package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.user.application.command.RegionCreateCommand;
import com.todak_todag.user_service.user.application.command.RegionUpdateActiveCommand;
import com.todak_todag.user_service.user.application.command.RegionUpdateCommand;
import com.todak_todag.user_service.user.application.result.RegionCreateResult;
import com.todak_todag.user_service.user.application.result.RegionUpdateActiveResult;
import com.todak_todag.user_service.user.application.result.RegionUpdateResult;
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

    // 지역 등록 메서드
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

    // 지역 상태 활성화 비활성화 메서드
    @Transactional
    public RegionUpdateActiveResult updateActive(
            RegionUpdateActiveCommand command
    ) {
        Region region = regionQueryRepository.findById(command.regionId())
                .orElseThrow(() ->
                        new BusinessException(RegionErrorCode.REGION_NOT_FOUND)
                );

        region.updateActive(command.isActive());

        log.info(
                "[Region] 지역 활성화 상태 변경 regionId={} isActive={}",
                region.getId(),
                region.isActive()
        );

        return RegionUpdateActiveResult.from(region);
    }


    // 지역 수정 메서드
    @Transactional
    public RegionUpdateResult updateRegion(
            RegionUpdateCommand command
    ) {
        // 최소 하나 이상의 수정 값이 전달되었는지 검증
        validateUpdateValue(command);

        Region region = regionQueryRepository.findById(command.regionId())
                .orElseThrow(() ->
                        new BusinessException(RegionErrorCode.REGION_NOT_FOUND)
                );

        // 행정 구역 코드가 변경되는 경우에 중복 검증
        validateDuplicateRegionCode(region, command.regionCode());

        region.update(
                command.province(),
                command.district(),
                command.regionCode()
        );

        log.info(
                "[Region] 지역 정보 수정 regionId={} regionCode={}",
                region.getId(),
                region.getRegionCode()
        );

        return RegionUpdateResult.from(region);
    }

    private void validateUpdateValue(RegionUpdateCommand command) {
        if (!command.hasUpdateValue()) {
            throw new BusinessException(
                    RegionErrorCode.REGION_UPDATE_VALUE_REQUIRED
            );
        }
    }

    private void validateDuplicateRegionCode(
            Region region,
            String regionCode
    ) {
        if (regionCode == null) {
            return;
        }

        // 본인코드랑 같은 코드를 수정을 위해 전달했을 때
        // 중복으로 처리하지 않도록
        if (region.getRegionCode().equals(regionCode)) {
            return;
        }

        if (regionQueryRepository.existsByRegionCode(regionCode)) {
            throw new BusinessException(
                    RegionErrorCode.REGION_DUPLICATE
            );
        }
    }
}
package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionQueryServiceTest {

    @Mock
    private RegionQueryRepository regionQueryRepository;

    @InjectMocks
    private RegionQueryService regionQueryService;

    @Test
    void 조회_결과가_없으면_빈_리스트를_반환한다() {
        // given
        when(regionQueryRepository.findAllAvailableRegions())
                .thenReturn(List.of());

        // when
        List<RegionFindAvailableResult> results =
                regionQueryService.findAvailableRegions();

        // then
        assertThat(results).isEmpty();
        verify(regionQueryRepository).findAllAvailableRegions();
    }

    @Test
    void 조회된_지역을_Result로_매핑한다() {
        // given
        UUID regionId = UUID.randomUUID();

        Region region = mock(Region.class);

        when(region.getId()).thenReturn(regionId);
        when(region.getProvince()).thenReturn("전라남도");
        when(region.getDistrict()).thenReturn("고흥군");
        when(region.getRegionCode()).thenReturn("4677000000");

        when(regionQueryRepository.findAllAvailableRegions())
                .thenReturn(List.of(region));

        // when
        List<RegionFindAvailableResult> results =
                regionQueryService.findAvailableRegions();

        // then
        assertThat(results).hasSize(1);

        RegionFindAvailableResult result = results.get(0);

        assertThat(result.regionId()).isEqualTo(regionId);
        assertThat(result.province()).isEqualTo("전라남도");
        assertThat(result.district()).isEqualTo("고흥군");
        assertThat(result.regionCode()).isEqualTo("4677000000");
    }
}
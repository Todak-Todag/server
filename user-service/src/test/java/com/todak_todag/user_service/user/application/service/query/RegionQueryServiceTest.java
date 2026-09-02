package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegionQueryServiceTest {

    @Mock
    private RegionQueryRepository regionQueryRepository;

    @InjectMocks
    private RegionQueryService regionQueryService;

    @Nested
    @DisplayName("서비스 가능 지역 목록 조회")
    class FindAvailableRegions {

        @Test
        @DisplayName("서비스 가능한 지역 목록을 조회한다")
        void findAvailableRegions_success() {
            // given
            UUID regionId = UUID.randomUUID();

            Region region = Mockito.mock(Region.class);

            given(region.getId()).willReturn(regionId);
            given(region.getProvince()).willReturn("전라남도");
            given(region.getDistrict()).willReturn("고흥군");
            given(region.getRegionCode()).willReturn("4677000000");

            given(regionQueryRepository.findAllAvailableRegions())
                    .willReturn(List.of(region));

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

        @Test
        @DisplayName("서비스 가능한 지역이 없으면 빈 목록을 반환한다")
        void findAvailableRegions_empty() {
            // given
            given(regionQueryRepository.findAllAvailableRegions())
                    .willReturn(List.of());

            // when
            List<RegionFindAvailableResult> results =
                    regionQueryService.findAvailableRegions();

            // then
            assertThat(results).isEmpty();
        }
    }
}
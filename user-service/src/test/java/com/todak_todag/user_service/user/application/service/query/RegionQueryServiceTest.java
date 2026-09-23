package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.application.result.RegionFindAdminResult;
import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.application.result.RegionFindDetailResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
            UUID regionId = UUID.randomUUID();
            Region region = Mockito.mock(Region.class);

            given(region.getId()).willReturn(regionId);
            given(region.getProvince()).willReturn("전라남도");
            given(region.getDistrict()).willReturn("고흥군");
            given(region.getRegionCode()).willReturn("4677000000");
            given(regionQueryRepository.findAllAvailableRegions())
                    .willReturn(List.of(region));

            List<RegionFindAvailableResult> results =
                    regionQueryService.findAvailableRegions();

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
            given(regionQueryRepository.findAllAvailableRegions())
                    .willReturn(List.of());

            List<RegionFindAvailableResult> results =
                    regionQueryService.findAvailableRegions();

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("관리자 지역 목록 조회")
    class FindAdminRegions {

        @Test
        @DisplayName("조건에 맞는 지역 목록을 페이징 조회한다")
        void findAdminRegions_success() {
            UUID regionId = UUID.randomUUID();

            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    0,
                    10,
                    "전라남도",
                    "고흥군",
                    "4677000000",
                    true
            );

            Region region = Mockito.mock(Region.class);

            given(region.getId()).willReturn(regionId);
            given(region.getProvince()).willReturn("전라남도");
            given(region.getDistrict()).willReturn("고흥군");
            given(region.getRegionCode()).willReturn("4677000000");
            given(region.isActive()).willReturn(true);

            Page<Region> regionPage = new PageImpl<>(
                    List.of(region)
            );

            given(regionQueryRepository.findAllByAdminConditions(
                    eq(query),
                    any(Pageable.class)
            )).willReturn(regionPage);

            Page<RegionFindAdminResult> results =
                    regionQueryService.findAdminRegions(query);

            assertThat(results.getContent()).hasSize(1);

            RegionFindAdminResult result = results.getContent().get(0);

            assertThat(result.regionId()).isEqualTo(regionId);
            assertThat(result.province()).isEqualTo("전라남도");
            assertThat(result.district()).isEqualTo("고흥군");
            assertThat(result.regionCode()).isEqualTo("4677000000");
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("페이징 값이 없으면 기본 페이징 조건을 사용한다")
        void findAdminRegions_defaultPageable() {
            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            given(regionQueryRepository.findAllByAdminConditions(
                    eq(query),
                    any(Pageable.class)
            )).willReturn(Page.empty());

            regionQueryService.findAdminRegions(query);

            ArgumentCaptor<Pageable> pageableCaptor =
                    ArgumentCaptor.forClass(Pageable.class);

            then(regionQueryRepository)
                    .should()
                    .findAllByAdminConditions(
                            eq(query),
                            pageableCaptor.capture()
                    );

            Pageable pageable = pageableCaptor.getValue();

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("createdAt"))
                    .isNotNull();
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("조회되는 지역이 없으면 빈 페이지를 반환한다")
        void findAdminRegions_empty() {
            RegionFindAdminQuery query = new RegionFindAdminQuery(
                    0,
                    10,
                    null,
                    null,
                    null,
                    null
            );

            given(regionQueryRepository.findAllByAdminConditions(
                    eq(query),
                    any(Pageable.class)
            )).willReturn(Page.empty());

            Page<RegionFindAdminResult> results =
                    regionQueryService.findAdminRegions(query);

            assertThat(results.getContent()).isEmpty();
            assertThat(results.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("서비스 가능 지역 존재 여부 확인")
    class ExistsAvailableRegion {

        @Test
        @DisplayName("활성화되어 있고 삭제되지 않은 지역이면 true를 반환한다")
        void existsAvailableRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionQueryRepository.existsAvailableRegion(regionId))
                    .willReturn(true);

            // when
            boolean result = regionQueryService.existsAvailableRegion(regionId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("서비스 가능한 지역이 아니면 false를 반환한다")
        void existsAvailableRegion_notAvailable() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionQueryRepository.existsAvailableRegion(regionId))
                    .willReturn(false);

            // when
            boolean result = regionQueryService.existsAvailableRegion(regionId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("지역 단건 조회")
    class FindRegion {

        @Test
        @DisplayName("지역 ID로 지역 상세 정보를 조회한다")
        void findRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();
            Region region = Mockito.mock(Region.class);

            given(region.getId()).willReturn(regionId);
            given(region.getProvince()).willReturn("전라남도");
            given(region.getDistrict()).willReturn("고흥군");
            given(region.getRegionCode()).willReturn("4677000000");
            given(region.isActive()).willReturn(true);

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.of(region));

            // when
            RegionFindDetailResult result =
                    regionQueryService.findRegion(regionId);

            // then
            assertThat(result.regionId()).isEqualTo(regionId);
            assertThat(result.province()).isEqualTo("전라남도");
            assertThat(result.district()).isEqualTo("고흥군");
            assertThat(result.regionCode()).isEqualTo("4677000000");
            assertThat(result.isActive()).isTrue();

            then(regionQueryRepository)
                    .should()
                    .findById(regionId);
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 REGION_NOT_FOUND 예외를 발생시킨다")
        void findRegion_notFound() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> regionQueryService.findRegion(regionId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(RegionErrorCode.REGION_NOT_FOUND);
                    });

            then(regionQueryRepository)
                    .should()
                    .findById(regionId);
        }
    }
}
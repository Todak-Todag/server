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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionCommandServiceTest {

    @Mock
    private RegionCommandRepository regionCommandRepository;

    @Mock
    private RegionQueryRepository regionQueryRepository;

    @InjectMocks
    private RegionCommandService regionCommandService;

    @Nested
    @DisplayName("지역 등록")
    class CreateRegion {

        @Test
        @DisplayName("지역을 비활성 상태로 등록한다")
        void createRegion_success() {
            UUID regionId = UUID.randomUUID();

            RegionCreateCommand command = new RegionCreateCommand(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            Region savedRegion = mock(Region.class);

            given(regionQueryRepository.existsByRegionCode(command.regionCode()))
                    .willReturn(false);

            given(savedRegion.getId())
                    .willReturn(regionId);

            given(regionCommandRepository.save(any(Region.class)))
                    .willReturn(savedRegion);

            RegionCreateResult result =
                    regionCommandService.createRegion(command);

            assertThat(result.regionId()).isEqualTo(regionId);

            then(regionQueryRepository)
                    .should()
                    .existsByRegionCode(command.regionCode());

            then(regionCommandRepository)
                    .should()
                    .save(any(Region.class));
        }

        @Test
        @DisplayName("동일한 행정구역 코드가 존재하면 등록할 수 없다")
        void createRegion_duplicateRegionCode() {
            RegionCreateCommand command = new RegionCreateCommand(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            given(regionQueryRepository.existsByRegionCode(command.regionCode()))
                    .willReturn(true);

            assertThatThrownBy(() ->
                    regionCommandService.createRegion(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(RegionErrorCode.REGION_DUPLICATE);
                    });

            then(regionQueryRepository)
                    .should()
                    .existsByRegionCode(command.regionCode());

            then(regionCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("지역 활성/비활성 상태 변경")
    class UpdateActive {

        @Test
        @DisplayName("지역의 활성 상태를 변경한다")
        void updateActive_success() {
            UUID regionId = UUID.randomUUID();

            RegionUpdateActiveCommand command =
                    new RegionUpdateActiveCommand(
                            regionId,
                            true
                    );

            Region region = mock(Region.class);

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.of(region));

            given(region.getId())
                    .willReturn(regionId);

            given(region.isActive())
                    .willReturn(true);

            RegionUpdateActiveResult result =
                    regionCommandService.updateActive(command);

            assertThat(result.regionId())
                    .isEqualTo(regionId);

            assertThat(result.isActive())
                    .isTrue();

            then(regionQueryRepository)
                    .should()
                    .findById(regionId);

            then(region)
                    .should()
                    .updateActive(true);
        }

        @Test
        @DisplayName("존재하지 않는 지역의 상태는 변경할 수 없다")
        void updateActive_regionNotFound() {
            UUID regionId = UUID.randomUUID();

            RegionUpdateActiveCommand command =
                    new RegionUpdateActiveCommand(
                            regionId,
                            true
                    );

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    regionCommandService.updateActive(command)
            )
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

            then(regionCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("지역 정보 수정")
    class UpdateRegion {

        @Test
        @DisplayName("지역 정보를 수정한다")
        void updateRegion_success() {
            // given
            UUID regionId = UUID.randomUUID();

            Region region = mock(Region.class);

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.of(region));
            given(region.getRegionCode())
                    .willReturn("4677000000");
            given(region.getId())
                    .willReturn(regionId);
            given(region.getProvince())
                    .willReturn("전라남도");
            given(region.getDistrict())
                    .willReturn("고흥군");
            given(region.isActive())
                    .willReturn(false);

            RegionUpdateCommand command = new RegionUpdateCommand(
                    regionId,
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            // when
            RegionUpdateResult result =
                    regionCommandService.updateRegion(command);

            // then
            verify(region).update(
                    "전라남도",
                    "고흥군",
                    "4677000000"
            );

            then(regionQueryRepository)
                    .should(never())
                    .existsByRegionCode(anyString());

            assertThat(result.regionId()).isEqualTo(regionId);
            assertThat(result.province()).isEqualTo("전라남도");
            assertThat(result.district()).isEqualTo("고흥군");
            assertThat(result.regionCode()).isEqualTo("4677000000");
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("변경할 행정구역코드가 이미 존재하면 예외가 발생한다")
        void updateRegion_duplicateRegionCode() {
            // given
            UUID regionId = UUID.randomUUID();

            Region region = mock(Region.class);

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.of(region));
            given(region.getRegionCode())
                    .willReturn("4677000000");
            given(regionQueryRepository.existsByRegionCode("4772000000"))
                    .willReturn(true);

            RegionUpdateCommand command = new RegionUpdateCommand(
                    regionId,
                    null,
                    null,
                    "4772000000"
            );

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> regionCommandService.updateRegion(command)
            );

            // then
            assertThat(exception.getErrorCode())
                    .isEqualTo(RegionErrorCode.REGION_DUPLICATE);

            verify(region, never())
                    .update(any(), any(), any());
        }

        @Test
        @DisplayName("수정할 값이 없으면 예외가 발생한다")
        void updateRegion_noUpdateValue() {
            // given
            UUID regionId = UUID.randomUUID();

            RegionUpdateCommand command = new RegionUpdateCommand(
                    regionId,
                    null,
                    null,
                    null
            );

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> regionCommandService.updateRegion(command)
            );

            // then
            assertThat(exception.getErrorCode())
                    .isEqualTo(RegionErrorCode.REGION_UPDATE_VALUE_REQUIRED);

            then(regionQueryRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 지역이면 예외가 발생한다")
        void updateRegion_notFound() {
            // given
            UUID regionId = UUID.randomUUID();

            given(regionQueryRepository.findById(regionId))
                    .willReturn(Optional.empty());

            RegionUpdateCommand command = new RegionUpdateCommand(
                    regionId,
                    "전라남도",
                    null,
                    null
            );

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> regionCommandService.updateRegion(command)
            );

            // then
            assertThat(exception.getErrorCode())
                    .isEqualTo(RegionErrorCode.REGION_NOT_FOUND);
        }
    }
}
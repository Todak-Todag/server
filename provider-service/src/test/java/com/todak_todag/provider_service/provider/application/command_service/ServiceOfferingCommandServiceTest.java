package com.todak_todag.provider_service.provider.application.command_service;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingCreateCommand;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.command.ServiceOfferingCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("제공 서비스 등록")
class ServiceOfferingCommandServiceTest {

    private final UUID providerId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID serviceOfferingId = UUID.randomUUID();

    @Mock
    private ServiceOfferingCommandRepository serviceOfferingCommandRepository;

    @Mock
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    @Mock
    private ProvideServiceQueryRepository provideServiceQueryRepository;

    @Mock
    private UserPort userPort;

    @InjectMocks
    private ServiceOfferingCommandService serviceOfferingCommandService;

    private ServiceOfferingCreateCommand command() {
        return new ServiceOfferingCreateCommand(providerId, provideServiceId);
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("User-Service에서 조회한 regionId와 함께 저장한다")
        void create_success() {
            Instant createdAt = Instant.now();
            ServiceOffering saved = Mockito.mock(ServiceOffering.class);

            given(saved.getId()).willReturn(serviceOfferingId);
            given(saved.getProviderId()).willReturn(providerId);
            given(saved.getCreatedAt()).willReturn(createdAt);

            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(true);
            given(serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId))
                    .willReturn(false);
            given(userPort.findRegionIdByUserId(providerId)).willReturn(regionId);
            given(serviceOfferingCommandRepository.save(any(ServiceOffering.class))).willReturn(saved);

            ServiceOfferingCreateResult result = serviceOfferingCommandService.create(command());

            assertThat(result.serviceOfferingId()).isEqualTo(serviceOfferingId);
            assertThat(result.providerId()).isEqualTo(providerId);
            assertThat(result.createdAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("존재하지 않는 서비스 종류면 PROVIDE_SERVICE_NOT_FOUND")
        void provideServiceNotFound() {
            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(false);

            assertThatThrownBy(() -> serviceOfferingCommandService.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_SERVICE_NOT_FOUND);

            verify(serviceOfferingCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 등록한 서비스 종류면 SERVICE_OFFERING_DUPLICATE")
        void duplicate() {
            given(provideServiceQueryRepository.existsById(provideServiceId)).willReturn(true);
            given(serviceOfferingQueryRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId))
                    .willReturn(true);

            assertThatThrownBy(() -> serviceOfferingCommandService.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.SERVICE_OFFERING_DUPLICATE);

            verify(userPort, never()).findRegionIdByUserId(any());
            verify(serviceOfferingCommandRepository, never()).save(any());
        }
    }
}
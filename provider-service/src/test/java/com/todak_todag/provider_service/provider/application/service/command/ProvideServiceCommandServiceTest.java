package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideServiceCreateCommand;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceCreateResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.command.ProvideServiceCommandRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("서비스 종류 등록 서비스")
class ProvideServiceCommandServiceTest {

    private static final String NAME = "방문간호";
    private static final String CONTENT = "간호사가 가정을 방문해 간호 서비스를 제공합니다.";

    @Mock
    private ProvideServiceCommandRepository provideServiceCommandRepository;

    @Mock
    private ProvideServiceQueryRepository provideServiceQueryRepository;

    @InjectMocks
    private ProvideServiceCommandService provideServiceCommandService;

    private ProvideServiceCreateCommand command() {
        return new ProvideServiceCreateCommand(NAME, CONTENT);
    }

    // 저장 시 DB가 채워주는 PK를 흉내낸다
    private ProvideService withGeneratedId(ProvideService provideService) {
        ReflectionTestUtils.setField(provideService, "id", UUID.randomUUID());
        return provideService;
    }

    @Nested
    @DisplayName("등록")
    class Create {

        @Test
        @DisplayName("중복되지 않은 서비스명이면 저장하고 등록 결과를 반환한다")
        void create_success() {
            given(provideServiceQueryRepository.existsByName(NAME)).willReturn(false);
            given(provideServiceCommandRepository.save(any(ProvideService.class)))
                    .willAnswer(invocation -> withGeneratedId(invocation.getArgument(0)));

            ProvideServiceCreateResult result = provideServiceCommandService.create(command());

            assertThat(result.provideServiceId()).isNotNull();
            assertThat(result.name()).isEqualTo(NAME);
            assertThat(result.content()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("요청 값이 엔티티에 그대로 매핑된다")
        void create_mapsFields() {
            given(provideServiceQueryRepository.existsByName(NAME)).willReturn(false);
            given(provideServiceCommandRepository.save(any(ProvideService.class)))
                    .willAnswer(invocation -> withGeneratedId(invocation.getArgument(0)));

            provideServiceCommandService.create(command());

            ArgumentCaptor<ProvideService> captor = ArgumentCaptor.forClass(ProvideService.class);
            verify(provideServiceCommandRepository).save(captor.capture());

            ProvideService saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo(NAME);
            assertThat(saved.getContent()).isEqualTo(CONTENT);
        }

        @Test
        @DisplayName("이미 등록된 서비스명이면 PROVIDE_SERVICE_DUPLICATE 예외가 발생하고 저장하지 않는다")
        void create_duplicateName_conflict() {
            given(provideServiceQueryRepository.existsByName(NAME)).willReturn(true);

            assertThatThrownBy(() -> provideServiceCommandService.create(command()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ProviderErrorCode.PROVIDE_SERVICE_DUPLICATE);

            verify(provideServiceCommandRepository, never()).save(any());
        }
    }
}
package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.domain.entity.ProvideService;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("서비스 종류 목록 조회 서비스")
class ProvideServiceQueryServiceTest {

    private static final String NAME = "방문간호";
    private static final String CONTENT = "간호사가 가정을 방문해 간호 서비스를 제공합니다.";

    private final Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Mock
    private ProvideServiceQueryRepository provideServiceQueryRepository;

    @InjectMocks
    private ProvideServiceQueryService provideServiceQueryService;

    private ProvideService mockProvideService(UUID id, Instant createdAt) {
        ProvideService provideService = mock(ProvideService.class);
        when(provideService.getId()).thenReturn(id);
        when(provideService.getName()).thenReturn(NAME);
        when(provideService.getContent()).thenReturn(CONTENT);
        when(provideService.getCreatedAt()).thenReturn(createdAt);
        return provideService;
    }

    @Test
    @DisplayName("조회 결과를 provideServiceName 필드로 매핑해 반환한다")
    void search_mapsToResult() {
        UUID provideServiceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-01T09:00:00Z");
        Page<ProvideService> page = new PageImpl<>(
                List.of(mockProvideService(provideServiceId, createdAt)), pageable, 1);

        given(provideServiceQueryRepository.findAll(pageable)).willReturn(page);

        Page<ProvideServiceSearchResult> results = provideServiceQueryService.search(pageable);

        assertThat(results.getTotalElements()).isEqualTo(1);
        ProvideServiceSearchResult result = results.getContent().get(0);
        assertThat(result.provideServiceId()).isEqualTo(provideServiceId);
        assertThat(result.provideServiceName()).isEqualTo(NAME);
        assertThat(result.content()).isEqualTo(CONTENT);
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 페이지를 반환한다")
    void search_empty() {
        Page<ProvideService> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(provideServiceQueryRepository.findAll(pageable)).willReturn(emptyPage);

        Page<ProvideServiceSearchResult> results = provideServiceQueryService.search(pageable);

        assertThat(results.getTotalElements()).isZero();
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    @DisplayName("전달받은 Pageable을 그대로 리포지토리에 위임한다")
    void search_delegatesPageable() {
        Page<ProvideService> page = new PageImpl<>(List.of(), pageable, 0);
        given(provideServiceQueryRepository.findAll(any())).willReturn(page);

        provideServiceQueryService.search(pageable);

        verify(provideServiceQueryRepository).findAll(pageable);
    }
}
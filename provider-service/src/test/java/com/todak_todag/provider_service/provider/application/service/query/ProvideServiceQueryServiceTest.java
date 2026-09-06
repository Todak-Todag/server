package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceInfoResult;
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

    private ProvideService mockProvideServiceInfo(UUID id, String name, String content) {
        ProvideService provideService = mock(ProvideService.class);
        when(provideService.getId()).thenReturn(id);
        when(provideService.getName()).thenReturn(name);
        when(provideService.getContent()).thenReturn(content);
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

    @Test
    @DisplayName("ID 목록으로 조회한 결과를 매핑해 반환한다")
    void findAllByIds_mapsToResult() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        ProvideService firstService = mockProvideServiceInfo(first, NAME, CONTENT);
        ProvideService secondService =
                mockProvideServiceInfo(second, "가사지원", "청소, 세탁 등 일상 가사 활동을 지원합니다.");

        given(provideServiceQueryRepository.findAllByIdIn(List.of(first, second)))
                .willReturn(List.of(firstService, secondService));

        List<ProvideServiceInfoResult> results =
                provideServiceQueryService.findAllByIds(List.of(first, second));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).provideServiceId()).isEqualTo(first);
        assertThat(results.get(0).name()).isEqualTo(NAME);
        assertThat(results.get(0).content()).isEqualTo(CONTENT);
        assertThat(results.get(1).name()).isEqualTo("가사지원");
    }

    @Test
    @DisplayName("존재하지 않는 ID가 섞여 있으면 조회된 것만 반환한다")
    void findAllByIds_ignoresMissingId() {
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();

        ProvideService existingService = mockProvideServiceInfo(existing, NAME, CONTENT);

        given(provideServiceQueryRepository.findAllByIdIn(List.of(existing, missing)))
                .willReturn(List.of(existingService));

        List<ProvideServiceInfoResult> results =
                provideServiceQueryService.findAllByIds(List.of(existing, missing));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).provideServiceId()).isEqualTo(existing);
    }

    @Test
    @DisplayName("조회된 서비스 종류가 없으면 빈 목록을 반환한다")
    void findAllByIds_empty() {
        UUID provideServiceId = UUID.randomUUID();

        given(provideServiceQueryRepository.findAllByIdIn(List.of(provideServiceId)))
                .willReturn(List.of());

        List<ProvideServiceInfoResult> results =
                provideServiceQueryService.findAllByIds(List.of(provideServiceId));

        assertThat(results).isEmpty();
    }
}
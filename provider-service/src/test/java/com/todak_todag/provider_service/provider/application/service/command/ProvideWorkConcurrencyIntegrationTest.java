package com.todak_todag.provider_service.provider.application.service.command;

import com.todak_todag.provider_service.global.common.UserRole;
import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkCreateCommand;
import com.todak_todag.provider_service.provider.application.command.ProvideWorkUpdateCommand;
import com.todak_todag.provider_service.provider.application.command.ServiceOfferingDeleteCommand;
import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaProvideWorkRepository;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// 락 효과는 Mockito로 증명할 수 없어 실제 PostgreSQL에서 독립 트랜잭션끼리 경합시킨다
// 테스트 클래스에 @Transactional을 붙이면 두 스레드가 같은 트랜잭션을 공유하지 않아 검증이 성립하지 않으므로 붙이지 않는다
// 경합은 타이밍에 따라 드러나므로 매 라운드 새 제공 서비스로 여러 번 반복한다
@DisplayName("제공 가능 일정 동시 변경 통합")
class ProvideWorkConcurrencyIntegrationTest extends ContainerTestSupport {

    private static final int ROUNDS = 10;
    private static final int MONDAY = 1;

    @Autowired
    private ProvideWorkCommandService provideWorkCommandService;

    @Autowired
    private ServiceOfferingCommandService serviceOfferingCommandService;

    @Autowired
    private JpaServiceOfferingRepository jpaServiceOfferingRepository;

    @Autowired
    private JpaProvideWorkRepository jpaProvideWorkRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ServiceOffering saveOffering(UUID providerId) {
        return jpaServiceOfferingRepository.saveAndFlush(
                ServiceOffering.of(providerId, UUID.randomUUID(), UUID.randomUUID()));
    }

    private ProvideWork saveWork(UUID serviceOfferingId, String startedAt, String finishedAt) {
        return jpaProvideWorkRepository.saveAndFlush(ProvideWork.of(
                serviceOfferingId, MONDAY, LocalTime.parse(startedAt), LocalTime.parse(finishedAt)));
    }

    // 두 작업을 같은 시점에 출발시키고 각 결과를 돌려받는다 (성공이면 null, 실패면 예외)
    // 교착이 생기면 무한 대기가 되므로 30초 상한을 둔다
    private List<Throwable> runConcurrently(Runnable first, Runnable second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Throwable>> futures = Stream.of(first, second)
                .map(task -> executor.submit(() -> {
                    start.await();
                    try {
                        task.run();
                        return (Throwable) null;
                    } catch (Throwable t) {
                        return t;
                    }
                }))
                .toList();

        start.countDown();

        List<Throwable> results = new ArrayList<>();
        for (Future<Throwable> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        return results;
    }

    private void assertErrorCode(Throwable thrown, ProviderErrorCode expected) {
        assertThat(thrown).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    @DisplayName("겹치는 등록 두 건이 동시에 들어오면 한 건만 저장된다")
    void create_overlapping_concurrently() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID providerId = UUID.randomUUID();
            UUID offeringId = saveOffering(providerId).getId();

            List<Throwable> results = runConcurrently(
                    () -> provideWorkCommandService.create(new ProvideWorkCreateCommand(
                            offeringId, providerId, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    () -> provideWorkCommandService.create(new ProvideWorkCreateCommand(
                            offeringId, providerId, MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0)))
            );

            assertThat(results).filteredOn(Objects::isNull).hasSize(1);
            assertErrorCode(results.stream().filter(Objects::nonNull).findFirst().orElseThrow(),
                    ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);
            assertThat(jpaProvideWorkRepository.findAllByServiceOfferingId(offeringId)).hasSize(1);
        }
    }

    @Test
    @DisplayName("각각은 유효하지만 함께 적용하면 겹치는 수정 두 건이 동시에 들어오면 한 건만 반영된다")
    void update_overlapping_concurrently() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID providerId = UUID.randomUUID();
            UUID offeringId = saveOffering(providerId).getId();
            UUID first = saveWork(offeringId, "09:00", "10:00").getId();
            UUID second = saveWork(offeringId, "14:00", "15:00").getId();

            // 11:00~13:00 과 12:00~14:00 은 각자 상대 일정의 원래 시간과는 겹치지 않지만 서로는 겹친다
            List<Throwable> results = runConcurrently(
                    () -> provideWorkCommandService.update(new ProvideWorkUpdateCommand(
                            offeringId, first, providerId, MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0))),
                    () -> provideWorkCommandService.update(new ProvideWorkUpdateCommand(
                            offeringId, second, providerId, MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0)))
            );

            assertThat(results).filteredOn(Objects::isNull).hasSize(1);
            assertErrorCode(results.stream().filter(Objects::nonNull).findFirst().orElseThrow(),
                    ProviderErrorCode.PROVIDE_WORK_TIME_OVERLAP);

            List<ProvideWork> works = jpaProvideWorkRepository.findAllByServiceOfferingId(offeringId);
            assertThat(works).noneMatch(a -> works.stream()
                    .anyMatch(b -> !a.getId().equals(b.getId())
                            && a.overlaps(b.getDay(), b.getStartedAt(), b.getFinishedAt())));
        }
    }

    @Test
    @DisplayName("일정 등록과 제공 서비스 삭제가 동시에 들어와도 삭제된 제공 서비스 아래에 활성 일정이 남지 않는다")
    void create_and_deleteOffering_concurrently() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID providerId = UUID.randomUUID();
            UUID offeringId = saveOffering(providerId).getId();

            List<Throwable> results = runConcurrently(
                    () -> provideWorkCommandService.create(new ProvideWorkCreateCommand(
                            offeringId, providerId, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))),
                    () -> serviceOfferingCommandService.delete(new ServiceOfferingDeleteCommand(
                            offeringId, providerId, UserRole.SERVICE_PROVIDER))
            );

            // 등록이 먼저 잠그면 저장 후 삭제가 함께 지우고, 삭제가 먼저 잠그면 등록은 제공 서비스를 찾지 못한다
            assertThat(results.get(1)).as("제공 서비스 삭제").isNull();
            if (results.get(0) != null) {
                assertErrorCode(results.get(0), ProviderErrorCode.SERVICE_OFFERING_NOT_FOUND);
            }

            assertThat(jpaServiceOfferingRepository.findById(offeringId)).isEmpty();
            assertThat(jpaProvideWorkRepository.findAllByServiceOfferingId(offeringId)).isEmpty();
        }
    }

    @Test
    @DisplayName("서로 다른 제공 서비스의 동시 등록은 서로 막지 않는다")
    void create_differentOfferings_concurrently() throws Exception {
        UUID providerId = UUID.randomUUID();
        UUID firstOfferingId = saveOffering(providerId).getId();
        UUID secondOfferingId = saveOffering(providerId).getId();

        List<Throwable> results = runConcurrently(
                () -> provideWorkCommandService.create(new ProvideWorkCreateCommand(
                        firstOfferingId, providerId, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))),
                () -> provideWorkCommandService.create(new ProvideWorkCreateCommand(
                        secondOfferingId, providerId, MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)))
        );

        assertThat(results).containsOnlyNulls();
    }
}
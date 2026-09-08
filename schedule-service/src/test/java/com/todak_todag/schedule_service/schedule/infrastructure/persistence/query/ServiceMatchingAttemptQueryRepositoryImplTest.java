package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, QueryDslConfig.class, ServiceMatchingAttemptQueryRepositoryImpl.class})
class ServiceMatchingAttemptQueryRepositoryImplTest extends PostgresTestSupport {

    private static final Pageable PAGEABLE = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired
    private ServiceMatchingAttemptQueryRepository serviceMatchingAttemptQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ServiceMatchingAttempt persistFailedAttempt(UUID servicePreferenceId, LocalDate date) {
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                null,
                date,
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.FAILED,
                "해당 날짜/시간대에 제공 가능한 서비스 제공자 없음",
                null,
                Instant.now()
        );
        return entityManager.persist(attempt);
    }

    private ServiceMatchingAttempt persistMatchedAttempt(UUID servicePreferenceId, LocalDate date) {
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                null,
                MatchingAttemptStatus.MATCHED,
                null,
                Instant.now(),
                null
        );
        return entityManager.persist(attempt);
    }

    private ServiceSchedule persistSchedule(UUID servicePreferenceId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                UUID.randomUUID(),
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        return entityManager.persist(schedule);
    }

    @Nested
    @DisplayName("status=FAILED 기본 조회")
    class failedSearchTest {

        @Test
        void 일정이_생성되지_않은_실패_내역만_반환한다() {
            // given
            UUID unresolved = UUID.randomUUID();
            UUID resolved = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceMatchingAttempt unresolvedAttempt = persistFailedAttempt(unresolved, date);

            persistFailedAttempt(resolved, date);
            persistMatchedAttempt(resolved, date.plusDays(1));
            persistSchedule(resolved, date.plusDays(1));

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(unresolved, resolved), MatchingAttemptStatus.FAILED, true, PAGEABLE
            );

            // then
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getId)
                    .containsExactly(unresolvedAttempt.getId());
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void 소프트_삭제된_일정은_생성되지_않은_것으로_취급한다() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceMatchingAttempt attempt = persistFailedAttempt(servicePreferenceId, date);

            ServiceSchedule schedule = persistSchedule(servicePreferenceId, date);
            schedule.markDeleted(SystemId.SYSTEM_USER_ID);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(servicePreferenceId), MatchingAttemptStatus.FAILED, true, PAGEABLE
            );

            // then
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getId)
                    .containsExactly(attempt.getId());
        }

        @Test
        void 소프트_삭제된_매칭_시도는_제외한다() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceMatchingAttempt attempt = persistFailedAttempt(servicePreferenceId, date);
            attempt.markDeleted(SystemId.SYSTEM_USER_ID);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(servicePreferenceId), MatchingAttemptStatus.FAILED, true, PAGEABLE
            );

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("status=MATCHED 조회")
    class matchedSearchTest {

        @Test
        void MATCHED_내역만_반환한다() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            persistFailedAttempt(servicePreferenceId, date);
            ServiceMatchingAttempt matched = persistMatchedAttempt(servicePreferenceId, date.plusDays(1));
            persistSchedule(servicePreferenceId, date.plusDays(1));

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(servicePreferenceId), MatchingAttemptStatus.MATCHED, false, PAGEABLE
            );

            // then
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getId)
                    .containsExactly(matched.getId());
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getStatus)
                    .containsOnly(MatchingAttemptStatus.MATCHED);
        }
    }

    @Nested
    @DisplayName("소유권 필터링")
    class ownershipTest {

        @Test
        void 담당하지_않는_servicePreferenceId의_매칭_시도는_결과에_포함되지_않는다() {
            // given
            UUID myPreference = UUID.randomUUID();
            UUID othersPreference = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            ServiceMatchingAttempt mine = persistFailedAttempt(myPreference, date);
            persistFailedAttempt(othersPreference, date);

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(myPreference), MatchingAttemptStatus.FAILED, true, PAGEABLE
            );

            // then
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getId)
                    .containsExactly(mine.getId());
            assertThat(result.getContent()).extracting(ServiceMatchingAttempt::getServicePreferenceId)
                    .doesNotContain(othersPreference);
        }

        @Test
        void servicePreferenceId_목록이_비어_있으면_예외를_던진다() {
            // given & when & then
            assertThatThrownBy(() -> serviceMatchingAttemptQueryRepository.search(
                    List.of(), MatchingAttemptStatus.FAILED, true, PAGEABLE
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("페이지네이션과 정렬")
    class paginationTest {

        @Test
        void size만큼_잘라_반환하고_전체_건수를_함께_알려준다() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();
            LocalDate date = LocalDate.now().plusDays(1);

            for (int i = 0; i < 3; i++) {
                persistFailedAttempt(servicePreferenceId, date.plusDays(i));
            }

            entityManager.flush();
            entityManager.clear();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(servicePreferenceId),
                    MatchingAttemptStatus.FAILED,
                    true,
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        void 조회_결과가_없으면_빈_페이지를_반환한다() {
            // given
            UUID servicePreferenceId = UUID.randomUUID();

            // when
            Page<ServiceMatchingAttempt> result = serviceMatchingAttemptQueryRepository.search(
                    List.of(servicePreferenceId), MatchingAttemptStatus.FAILED, true, PAGEABLE
            );

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}

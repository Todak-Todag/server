package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.global.config.QueryDslConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceMatchingAttemptCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.query.ServiceMatchingAttemptQueryRepositoryImpl;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        JpaConfig.class,
        QueryDslConfig.class,
        ServiceMatchingAttemptCommandRepositoryImpl.class,
        ServiceMatchingAttemptQueryRepositoryImpl.class
})
class ServiceMatchingAttemptRepositoryTest extends PostgresTestSupport {

    @Autowired
    private ServiceMatchingAttemptCommandRepository serviceMatchingAttemptCommandRepository;

    @Autowired
    private ServiceMatchingAttemptQueryRepository serviceMatchingAttemptQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 매칭_성공_기록을_저장하면_모든_필드가_그대로_조회된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID provideServiceId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        Instant matchedAt = Instant.now();

        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                carePlanId,
                regionId,
                provideServiceId,
                servicePreferenceId,
                serviceOfferingId,
                date,
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.MATCHED,
                null,
                matchedAt,
                null
        );

        // when
        ServiceMatchingAttempt saved = serviceMatchingAttemptCommandRepository.save(attempt);
        entityManager.flush();
        entityManager.clear();

        Optional<ServiceMatchingAttempt> found = serviceMatchingAttemptQueryRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCarePlanId()).isEqualTo(carePlanId);
        assertThat(found.get().getRegionId()).isEqualTo(regionId);
        assertThat(found.get().getProvideServiceId()).isEqualTo(provideServiceId);
        assertThat(found.get().getServicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(found.get().getServiceOfferingId()).isEqualTo(serviceOfferingId);
        assertThat(found.get().getDate()).isEqualTo(date);
        assertThat(found.get().getPreferredTimeSlot()).isEqualTo(PreferredTimeSlot.MORNING);
        assertThat(found.get().getStatus()).isEqualTo(MatchingAttemptStatus.MATCHED);
        assertThat(found.get().getFailureReason()).isNull();
        assertThat(found.get().getFailedAt()).isNull();

        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getCreatedBy()).isEqualTo(SystemId.SYSTEM_USER_ID);
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void 매칭_실패_기록은_실패_사유와_실패_일시가_함께_저장된다() {
        // given
        Instant failedAt = Instant.now();
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDate.now().plusDays(1),
                PreferredTimeSlot.AFTERNOON,
                MatchingAttemptStatus.FAILED,
                "지역 내 가능한 서비스 제공자가 없습니다",
                null,
                failedAt
        );

        // when
        ServiceMatchingAttempt saved = serviceMatchingAttemptCommandRepository.save(attempt);
        entityManager.flush();
        entityManager.clear();

        Optional<ServiceMatchingAttempt> found = serviceMatchingAttemptQueryRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(MatchingAttemptStatus.FAILED);
        assertThat(found.get().getFailureReason()).isEqualTo("지역 내 가능한 서비스 제공자가 없습니다");
        assertThat(found.get().getMatchedAt()).isNull();
        assertThat(found.get().getServiceOfferingId()).isNull();
        assertThat(found.get().getPreferredTimeSlot()).isEqualTo(PreferredTimeSlot.AFTERNOON);
    }

    @Test
    void 일정이_아직_없는_FAILED_이력만_미해소로_집계된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID unresolvedPreferenceId = UUID.randomUUID();
        UUID resolvedPreferenceId = UUID.randomUUID();

        serviceMatchingAttemptCommandRepository.save(failedAttempt(carePlanId, unresolvedPreferenceId));
        serviceMatchingAttemptCommandRepository.save(failedAttempt(carePlanId, resolvedPreferenceId));
        entityManager.persist(scheduleOf(carePlanId, resolvedPreferenceId));
        entityManager.flush();
        entityManager.clear();

        // when
        long unresolved = serviceMatchingAttemptCommandRepository.countUnresolvedFailed(carePlanId);

        // then
        assertThat(unresolved).isEqualTo(1L);
    }

    @Test
    void 소프트_삭제된_FAILED_이력은_미해소_집계에서_제외된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        ServiceMatchingAttempt attempt =
                serviceMatchingAttemptCommandRepository.save(failedAttempt(carePlanId, UUID.randomUUID()));
        attempt.markDeleted(SystemId.SYSTEM_USER_ID);
        entityManager.flush();
        entityManager.clear();

        // when
        long unresolved = serviceMatchingAttemptCommandRepository.countUnresolvedFailed(carePlanId);

        // then
        assertThat(unresolved).isZero();
    }

    @Test
    void 매칭에_성공한_이력은_일정_유무와_무관하게_미해소로_집계되지_않는다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        serviceMatchingAttemptCommandRepository.save(
                ServiceMatchingAttempt.record(
                        carePlanId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        LocalDate.now().plusDays(1),
                        null,
                        MatchingAttemptStatus.MATCHED,
                        null,
                        Instant.now(),
                        null
                )
        );
        entityManager.flush();
        entityManager.clear();

        // when
        long unresolved = serviceMatchingAttemptCommandRepository.countUnresolvedFailed(carePlanId);

        // then
        assertThat(unresolved).isZero();
    }

    private ServiceMatchingAttempt failedAttempt(UUID carePlanId, UUID servicePreferenceId) {
        return ServiceMatchingAttempt.record(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                servicePreferenceId,
                null,
                LocalDate.now().plusDays(1),
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.FAILED,
                "NO_AVAILABLE_PROVIDER",
                null,
                Instant.now()
        );
    }

    private ServiceSchedule scheduleOf(UUID carePlanId, UUID servicePreferenceId) {
        LocalDate date = LocalDate.now().plusDays(2);

        return ServiceSchedule.confirm(
                carePlanId,
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
    }

    @Test
    void 소프트_삭제된_기록은_단건_조회에서_제외된다() {
        // given
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                PreferredTimeSlot.MORNING,
                MatchingAttemptStatus.MATCHED,
                null,
                Instant.now(),
                null
        );
        ServiceMatchingAttempt saved = serviceMatchingAttemptCommandRepository.save(attempt);
        saved.markDeleted(SystemId.SYSTEM_USER_ID);

        // when
        entityManager.flush();
        entityManager.clear();

        Optional<ServiceMatchingAttempt> found = serviceMatchingAttemptQueryRepository.findById(saved.getId());

        // then
        assertThat(found).isEmpty();
    }
}

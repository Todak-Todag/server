package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceMatchingAttemptCommandRepository;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceMatchingAttemptCommandRepositoryImpl;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.query.ServiceMatchingAttemptQueryRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        JpaConfig.class,
        ServiceMatchingAttemptCommandRepositoryImpl.class,
        ServiceMatchingAttemptQueryRepositoryImpl.class
})
class ServiceMatchingAttemptRepositoryTest {

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
        LocalDateTime matchedAt = LocalDateTime.now();

        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                carePlanId,
                regionId,
                provideServiceId,
                servicePreferenceId,
                serviceOfferingId,
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
        assertThat(found.get().getStatus()).isEqualTo(MatchingAttemptStatus.MATCHED);
        assertThat(found.get().getFailureReason()).isNull();
        assertThat(found.get().getFailedAt()).isNull();

        // 공통 감사 필드(BaseAuditableEntity)가 JPA Auditing으로 채워지는지 함께 확인
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getCreatedBy()).isEqualTo(SystemId.SYSTEM_USER_ID);
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void 매칭_실패_기록은_실패_사유와_실패_일시가_함께_저장된다() {
        // given
        LocalDateTime failedAt = LocalDateTime.now();
        ServiceMatchingAttempt attempt = ServiceMatchingAttempt.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
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
                MatchingAttemptStatus.MATCHED,
                null,
                LocalDateTime.now(),
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

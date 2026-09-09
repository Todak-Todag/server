package com.todak_todag.discharge_service.discharge.infrastructure.persistence.query;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.global.config.JpaConfig;
import com.todak_todag.discharge_service.global.config.QueryDslConfig;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        JpaConfig.class,
        QueryDslConfig.class,
        DischargeQueryRepositoryImpl.class,
        DischargeQueryRepositoryImplTest.TestAuditConfig.class
})
class DischargeQueryRepositoryImplTest {

    @Autowired
    private DischargeQueryRepository dischargeQueryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("퇴원건 목록 조회")
    class SearchTest {

        @Test
        @DisplayName("병원 담당자는 자신이 작성한 퇴원건만 조회한다")
        void search_returnsOnlyOwnDischarges() {
            UUID myStaffId = UUID.randomUUID();
            UUID otherStaffId = UUID.randomUUID();

            Discharge mine = persistDischarge(
                    UUID.randomUUID(),
                    myStaffId,
                    "내 병원",
                    LocalDate.of(2026, 9, 10)
            );

            Discharge others = persistDischarge(
                    UUID.randomUUID(),
                    otherStaffId,
                    "다른 병원",
                    LocalDate.of(2026, 9, 10)
            );

            entityManager.flush();
            entityManager.clear();

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            myStaffId,
                            null,
                            null,
                            PageRequest.of(0, 10)
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(mine.getId())
                    .doesNotContain(others.getId());
        }

        @Test
        @DisplayName("status를 전달하면 해당 상태의 퇴원건만 조회한다")
        void search_withStatusFilter_returnsOnlyMatchingStatus() {
            UUID staffId = UUID.randomUUID();

            Discharge scheduled = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 10)
            );

            Discharge canceled = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 11)
            );

            setStatus(
                    canceled,
                    DischargeStatus.CANCELED
            );

            entityManager.flush();
            entityManager.clear();

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            staffId,
                            DischargeStatus.SCHEDULED,
                            null,
                            PageRequest.of(0, 10)
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(scheduled.getId())
                    .doesNotContain(canceled.getId());
        }

        @Test
        @DisplayName("scheduledDate를 전달하면 해당 예정 퇴원일의 퇴원건만 조회한다")
        void search_withScheduledDateFilter_returnsOnlyMatchingDate() {
            UUID staffId = UUID.randomUUID();

            LocalDate targetDate =
                    LocalDate.of(2026, 9, 10);

            LocalDate otherDate =
                    LocalDate.of(2026, 9, 11);

            Discharge target = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    targetDate
            );

            Discharge other = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    otherDate
            );

            entityManager.flush();
            entityManager.clear();

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            staffId,
                            null,
                            targetDate,
                            PageRequest.of(0, 10)
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(target.getId())
                    .doesNotContain(other.getId());
        }

        @Test
        @DisplayName("status와 scheduledDate를 동시에 적용할 수 있다")
        void search_withStatusAndDateFilter_returnsOnlyMatchingDischarge() {
            UUID staffId = UUID.randomUUID();

            LocalDate targetDate =
                    LocalDate.of(2026, 9, 10);

            Discharge matched = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    targetDate
            );

            Discharge wrongDate = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    targetDate.plusDays(1)
            );

            Discharge wrongStatus = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    targetDate
            );

            setStatus(
                    wrongStatus,
                    DischargeStatus.CANCELED
            );

            entityManager.flush();
            entityManager.clear();

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            staffId,
                            DischargeStatus.SCHEDULED,
                            targetDate,
                            PageRequest.of(0, 10)
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(matched.getId())
                    .doesNotContain(
                            wrongDate.getId(),
                            wrongStatus.getId()
                    );
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 페이지를 반환한다")
        void search_noResult_returnsEmptyPage() {
            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            UUID.randomUUID(),
                            null,
                            null,
                            PageRequest.of(0, 10)
                    );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("페이지네이션이 정상 동작한다")
        void search_pagination_works() {
            UUID staffId = UUID.randomUUID();

            for (int i = 0; i < 3; i++) {
                persistDischarge(
                        UUID.randomUUID(),
                        staffId,
                        "한샘병원",
                        LocalDate.of(2026, 9, 10 + i)
                );
            }

            entityManager.flush();
            entityManager.clear();

            Page<Discharge> firstPage =
                    dischargeQueryRepository.search(
                            staffId,
                            null,
                            null,
                            PageRequest.of(0, 2)
                    );

            Page<Discharge> secondPage =
                    dischargeQueryRepository.search(
                            staffId,
                            null,
                            null,
                            PageRequest.of(1, 2)
                    );

            assertThat(firstPage.getContent()).hasSize(2);
            assertThat(firstPage.getTotalElements()).isEqualTo(3);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);

            assertThat(secondPage.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("createdAt 기준 오래된순 ASC 정렬이 정상 동작한다")
        void search_sortAscending_ordersByCreatedAtAsc() {
            UUID staffId = UUID.randomUUID();

            Discharge first = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 10)
            );

            Discharge second = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 11)
            );

            entityManager.flush();

            setCreatedAt(
                    first,
                    Instant.parse("2026-09-01T01:00:00Z")
            );

            setCreatedAt(
                    second,
                    Instant.parse("2026-09-02T01:00:00Z")
            );

            entityManager.clear();

            Pageable pageable =
                    PageRequest.of(
                            0,
                            10,
                            Sort.by(
                                    Sort.Direction.ASC,
                                    "createdAt"
                            )
                    );

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            staffId,
                            null,
                            null,
                            pageable
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(
                            first.getId(),
                            second.getId()
                    );
        }

        @Test
        @DisplayName("createdAt 기준 최신순 DESC 정렬이 정상 동작한다")
        void search_sortDescending_ordersByCreatedAtDesc() {
            UUID staffId = UUID.randomUUID();

            Discharge first = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 10)
            );

            Discharge second = persistDischarge(
                    UUID.randomUUID(),
                    staffId,
                    "한샘병원",
                    LocalDate.of(2026, 9, 11)
            );

            setCreatedAt(
                    first,
                    Instant.parse("2026-09-01T01:00:00Z")
            );

            setCreatedAt(
                    second,
                    Instant.parse("2026-09-02T01:00:00Z")
            );

            entityManager.flush();
            entityManager.clear();

            Pageable pageable =
                    PageRequest.of(
                            0,
                            10,
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "createdAt"
                            )
                    );

            Page<Discharge> result =
                    dischargeQueryRepository.search(
                            staffId,
                            null,
                            null,
                            pageable
                    );

            assertThat(result.getContent())
                    .extracting(Discharge::getId)
                    .containsExactly(
                            second.getId(),
                            first.getId()
                    );
        }
    }

    private Discharge persistDischarge(
            UUID patientId,
            UUID hospitalStaffId,
            String hospitalName,
            LocalDate scheduledDate
    ) {
        Discharge discharge =
                Discharge.create(
                        patientId,
                        hospitalStaffId,
                        hospitalName,
                        scheduledDate
                );

        entityManager.persist(discharge);

        return discharge;
    }

    private void setStatus(
            Discharge discharge,
            DischargeStatus status
    ) {
        try {
            Field field =
                    Discharge.class.getDeclaredField("status");

            field.setAccessible(true);
            field.set(
                    discharge,
                    status
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setCreatedAt(
            Discharge discharge,
            Instant createdAt
    ) {
        entityManager.getEntityManager()
                .createNativeQuery("""
                    UPDATE discharge_schema.p_discharges
                    SET created_at = :createdAt
                    WHERE id = :id
                    """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", discharge.getId())
                .executeUpdate();
    }

    @TestConfiguration
    static class TestAuditConfig {

        @Bean
        AuditorAware<UUID> auditorAware() {
            return () -> Optional.of(
                    UUID.fromString("00000000-0000-0000-0000-000000000001")
            );
        }
    }
}
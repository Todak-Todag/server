package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 겹침 판정을 메모리 비교에서 DB 쿼리(existsOverlapped)로 옮기면서, 판정 규칙의 검증도 이 계층으로 내려왔다
// 요일 구분·시각 경계·자기 자신 제외·소프트 삭제 반영을 실제 PostgreSQL에서 확인한다
@DisplayName("제공 가능 일정 겹침 조회")
class ProvideWorkOverlapQueryIntegrationTest extends ContainerTestSupport {

    private static final int MONDAY = 1;
    private static final int TUESDAY = 2;

    @Autowired
    private JpaProvideWorkRepository jpaProvideWorkRepository;

    @Autowired
    private JpaServiceOfferingRepository jpaServiceOfferingRepository;

    private UUID serviceOfferingId;

    // 라운드마다 새 제공 서비스를 써서 이전 테스트가 남긴 일정에 영향받지 않게 한다
    @BeforeEach
    void setUp() {
        ServiceOffering offering = jpaServiceOfferingRepository.saveAndFlush(
                ServiceOffering.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        serviceOfferingId = offering.getId();
    }

    private ProvideWork saveWork(int day, String startedAt, String finishedAt) {
        return jpaProvideWorkRepository.saveAndFlush(ProvideWork.of(
                serviceOfferingId, day, LocalTime.parse(startedAt), LocalTime.parse(finishedAt)));
    }

    private boolean exists(int day, String startedAt, String finishedAt) {
        return jpaProvideWorkRepository.existsOverlapped(
                serviceOfferingId, null, day, LocalTime.parse(startedAt), LocalTime.parse(finishedAt));
    }

    private boolean existsExcluding(UUID excludedId, int day, String startedAt, String finishedAt) {
        return jpaProvideWorkRepository.existsOverlapped(
                serviceOfferingId, excludedId, day, LocalTime.parse(startedAt), LocalTime.parse(finishedAt));
    }

    @Nested
    @DisplayName("겹친다고 판정한다")
    class Overlapped {

        @Test
        @DisplayName("기존 일정의 뒷부분과 물리면 겹친다")
        void partialOverlap() {
            saveWork(MONDAY, "09:00", "13:00");

            assertThat(exists(MONDAY, "12:00", "15:00")).isTrue();
        }

        @Test
        @DisplayName("기존 일정을 완전히 감싸면 겹친다")
        void contains() {
            saveWork(MONDAY, "10:00", "11:00");

            assertThat(exists(MONDAY, "09:00", "13:00")).isTrue();
        }

        @Test
        @DisplayName("기존 일정 안에 완전히 들어가도 겹친다")
        void contained() {
            saveWork(MONDAY, "09:00", "13:00");

            assertThat(exists(MONDAY, "10:00", "11:00")).isTrue();
        }

        @Test
        @DisplayName("시작과 종료가 같으면 겹친다")
        void identical() {
            saveWork(MONDAY, "09:00", "13:00");

            assertThat(exists(MONDAY, "09:00", "13:00")).isTrue();
        }
    }

    @Nested
    @DisplayName("겹치지 않는다고 판정한다")
    class NotOverlapped {

        @Test
        @DisplayName("요일이 다르면 시간이 같아도 겹치지 않는다")
        void differentDay() {
            saveWork(MONDAY, "09:00", "13:00");

            assertThat(exists(TUESDAY, "09:00", "13:00")).isFalse();
        }

        @Test
        @DisplayName("기존 일정의 종료 시각에 시작하면 겹치지 않는다")
        void adjacentAfter() {
            saveWork(MONDAY, "09:00", "13:00");

            assertThat(exists(MONDAY, "13:00", "18:00")).isFalse();
        }

        @Test
        @DisplayName("기존 일정의 시작 시각에 끝나면 겹치지 않는다")
        void adjacentBefore() {
            saveWork(MONDAY, "13:00", "18:00");

            assertThat(exists(MONDAY, "09:00", "13:00")).isFalse();
        }

        @Test
        @DisplayName("다른 제공 서비스의 일정은 겹침 대상이 아니다")
        void otherServiceOffering() {
            ServiceOffering other = jpaServiceOfferingRepository.saveAndFlush(
                    ServiceOffering.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
            jpaProvideWorkRepository.saveAndFlush(ProvideWork.of(
                    other.getId(), MONDAY, LocalTime.parse("09:00"), LocalTime.parse("13:00")));

            assertThat(exists(MONDAY, "09:00", "13:00")).isFalse();
        }

        @Test
        @DisplayName("일정이 하나도 없으면 겹치지 않는다")
        void empty() {
            assertThat(exists(MONDAY, "09:00", "13:00")).isFalse();
        }
    }

    @Nested
    @DisplayName("제외 대상과 소프트 삭제")
    class Excluded {

        @Test
        @DisplayName("수정 대상 자신은 겹침 대상에서 빠진다")
        void excludesItself() {
            ProvideWork target = saveWork(MONDAY, "09:00", "13:00");

            // 시간을 그대로 두고 수정해도 자기 자신 때문에 겹친다고 나오면 안 된다
            assertThat(existsExcluding(target.getId(), MONDAY, "09:00", "13:00")).isFalse();
        }

        @Test
        @DisplayName("제외 대상이 있어도 다른 일정과 겹치면 겹친다고 판정한다")
        void excludesOnlyItself() {
            ProvideWork target = saveWork(MONDAY, "09:00", "13:00");
            saveWork(MONDAY, "13:00", "18:00");

            assertThat(existsExcluding(target.getId(), MONDAY, "12:00", "15:00")).isTrue();
        }

        // 부하 테스트에서 삭제 실패가 재등록을 막았던 지점이다
        // @SQLRestriction("deleted_at is null")이 JPQL에도 적용되는지 확인한다
        @Test
        @DisplayName("소프트 삭제된 일정은 재등록을 막지 않는다")
        void ignoresSoftDeleted() {
            ProvideWork deleted = saveWork(MONDAY, "09:00", "13:00");
            deleted.markDeleted(UUID.randomUUID());
            jpaProvideWorkRepository.saveAndFlush(deleted);

            assertThat(exists(MONDAY, "09:00", "13:00")).isFalse();
        }
    }
}

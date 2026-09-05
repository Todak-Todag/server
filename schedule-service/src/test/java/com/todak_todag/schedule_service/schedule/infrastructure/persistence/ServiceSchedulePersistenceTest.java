package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.global.config.JpaConfig;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.command.ServiceScheduleCommandRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.command.ServiceScheduleCommandRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaConfig.class, ServiceScheduleCommandRepositoryImpl.class})
class ServiceSchedulePersistenceTest {

    @Autowired
    private ServiceScheduleCommandRepository serviceScheduleCommandRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 서비스_일정을_저장하면_carePlanId가_함께_저장되고_조회된다() {
        // given
        UUID carePlanId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                servicePreferenceId,
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );

        // when
        ServiceSchedule saved = serviceScheduleCommandRepository.save(schedule);
        entityManager.flush();
        entityManager.clear();

        Optional<ServiceSchedule> found = serviceScheduleCommandRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCarePlanId()).isEqualTo(carePlanId);
        assertThat(found.get().getServicePreferenceId()).isEqualTo(servicePreferenceId);
    }
}

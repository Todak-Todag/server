package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.schedule_service.schedule.domain.entity.QServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceScheduleQueryRepositoryImpl implements ServiceScheduleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Override
    public Optional<ServiceSchedule> findById(UUID serviceScheduleId) {
        return springDataServiceScheduleRepository.findByIdAndDeletedAtIsNull(serviceScheduleId);
    }

    @Override
    public List<ServiceSchedule> findSchedules(
            List<UUID> serviceOfferingIds,
            LocalDate startDate,
            LocalDate endDate,
            List<ScheduleStatus> statuses
    ) {
        QServiceSchedule schedule = QServiceSchedule.serviceSchedule;

        return jpaQueryFactory
                .selectFrom(schedule)
                // serviceOfferingId IN / date BETWEEN / status IN / deletedAt IS NULL 조건으로 검색
                .where(
                        schedule.serviceOfferingId.in(serviceOfferingIds),
                        schedule.date.between(startDate, endDate),
                        schedule.status.in(statuses),
                        schedule.deletedAt.isNull()
                )
                .fetch();
    }
}

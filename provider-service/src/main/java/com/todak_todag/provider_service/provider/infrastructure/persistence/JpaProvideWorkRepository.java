package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ProvideWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface JpaProvideWorkRepository extends JpaRepository<ProvideWork, UUID> {

    List<ProvideWork> findAllByServiceOfferingId(UUID serviceOfferingId);

    List<ProvideWork> findAllByServiceOfferingIdIn(List<UUID> serviceOfferingIds);

    // 같은 제공 서비스 안에서 요일이 같고 시간이 겹치는 일정이 있는지 (수정 시 자기 자신은 제외)
    // 겹침 판정은 ProvideWork.overlaps 와 동일하다
    @Query("""
            select count(pw) > 0
            from ProvideWork pw
            where pw.serviceOfferingId = :serviceOfferingId
              and (:excludedProvideWorkId is null or pw.id <> :excludedProvideWorkId)
              and pw.day = :day
              and pw.startedAt < :finishedAt
              and :startedAt < pw.finishedAt
            """)
    boolean existsOverlapped(
            @Param("serviceOfferingId") UUID serviceOfferingId,
            @Param("excludedProvideWorkId") UUID excludedProvideWorkId,
            @Param("day") Integer day,
            @Param("startedAt") LocalTime startedAt,
            @Param("finishedAt") LocalTime finishedAt
    );
}
package com.todak_todag.schedule_service.schedule.infrastructure.persistence;

import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Spring Data JPA를 통한 기본 CRUD 전용 인터페이스
public interface SpringDataServiceMatchingAttemptRepository extends JpaRepository<ServiceMatchingAttempt, UUID> {

    // 소프트 삭제된 기록은 조회 대상에서 제외
    Optional<ServiceMatchingAttempt> findByIdAndDeletedAtIsNull(UUID id);
}

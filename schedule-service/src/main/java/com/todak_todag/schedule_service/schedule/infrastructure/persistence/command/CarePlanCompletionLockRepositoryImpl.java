package com.todak_todag.schedule_service.schedule.infrastructure.persistence.command;

import com.todak_todag.schedule_service.schedule.domain.repository.command.CarePlanCompletionLockRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Postgres advisory lock 기반 구현
// 잠글 대상인 케어플랜은 이 서비스에 테이블이 없어(논리 FK뿐) 잠글 로우 자체가 없고,
// 초기 매칭 실패 건은 일정 레코드조차 없어 로우 단위 비관적 락으로는 판정 구간을 덮을 수 없음
@Repository
public class CarePlanCompletionLockRepositoryImpl implements CarePlanCompletionLockRepository {

    @PersistenceContext
    private EntityManager entityManager;

    // xact 계열이라 커밋/롤백 시 자동 해제 (해제 누락으로 인한 데드락 위험 없음)
    @Override
    public void lockForCompletionCheck(UUID carePlanId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", toLockKey(carePlanId))
                .getSingleResult();
    }

    // advisory lock 키는 bigint라 UUID(128bit)를 축약
    // 해시 충돌이 나도 무관한 케어플랜 둘이 잠시 직렬화될 뿐 판정 정확성에는 영향이 없음
    private long toLockKey(UUID carePlanId) {
        return carePlanId.getMostSignificantBits() ^ carePlanId.getLeastSignificantBits();
    }
}

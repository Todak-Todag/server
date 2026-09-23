package com.todak_todag.discharge_service.discharge.infrastructure.persistence;

import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DischargeOptimisticLockTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("동일한 퇴원건을 동시에 수정하면 늦은 요청에서 낙관적 락 충돌이 발생한다")
    void concurrentUpdateCausesOptimisticLockException() {

        UUID dischargeId =
                createDischarge();

        EntityManager em1 =
                entityManagerFactory.createEntityManager();

        EntityManager em2 =
                entityManagerFactory.createEntityManager();

        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();

            Discharge dischargeA =
                    em1.find(
                            Discharge.class,
                            dischargeId
                    );

            Discharge dischargeB =
                    em2.find(
                            Discharge.class,
                            dischargeId
                    );

            dischargeA.complete(
                    LocalDate.now()
            );

            em1.flush();
            em1.getTransaction().commit();

            dischargeB.update(
                    DischargeStatus.CANCELED,
                    null
            );

            assertThrows(
                    OptimisticLockException.class,
                    em2::flush
            );

            em2.getTransaction().rollback();

        } finally {

            if (em1.isOpen()) {
                em1.close();
            }

            if (em2.isOpen()) {
                em2.close();
            }
        }
    }

    private UUID createDischarge() {

        EntityManager em =
                entityManagerFactory.createEntityManager();

        try {
            em.getTransaction().begin();

            Discharge discharge =
                    Discharge.create(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "테스트 병원",
                            LocalDate.now().plusDays(1)
                    );

            em.persist(discharge);
            em.flush();
            em.getTransaction().commit();

            return discharge.getId();

        } finally {

            if (em.isOpen()) {
                em.close();
            }
        }
    }
}
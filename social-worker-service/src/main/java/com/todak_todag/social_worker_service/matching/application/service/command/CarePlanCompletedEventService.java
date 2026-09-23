package com.todak_todag.social_worker_service.matching.application.service.command;

import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.domain.entity.MatchingStatus;
import com.todak_todag.social_worker_service.matching.domain.entity.SocialWorkerMatchingResult;
import com.todak_todag.social_worker_service.matching.domain.repository.command.SocialWorkerMatchingCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarePlanCompletedEventService {

    private final SocialWorkerMatchingCommandRepository matchingCommandRepository;

    @Transactional
    public void handle(
            CarePlanCompletedEvent event
    ) {

        SocialWorkerMatchingResult matchingResult =
                matchingCommandRepository
                        .findByPatientIdAndStatus(
                                event.patientId(),
                                MatchingStatus.ACTIVE
                        )
                        .orElse(null);

        if (matchingResult == null) {

            log.info(
                    "[SocialWorkerMatching] 종료할 ACTIVE 매칭 없음 "
                            + "eventId={}, carePlanId={}, patientId={}",
                    event.eventId(),
                    event.carePlanId(),
                    event.patientId()
            );

            return;
        }

        matchingResult.setStatus(
                MatchingStatus.ENDED
        );

        log.info(
                "[SocialWorkerMatching] Care Plan 완료에 따라 사회복지사 매칭 종료 "
                        + "eventId={}, carePlanId={}, matchingResultId={}, patientId={}",
                event.eventId(),
                event.carePlanId(),
                matchingResult.getMatchingResultId(),
                event.patientId()
        );
    }
}
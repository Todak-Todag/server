package com.todak_todag.user_service.user.domain.repository.query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsentDocumentQueryRepository {

    // 현재 적용 중인 전체 약관 조회
    List<ConsentDocumentCurrentView> findAllCurrent(
            LocalDateTime now
    );

    // 회원가입 시 전달받은 약관 버전이 현재 적용 중인지 확인하기 위한 조회
    List<ConsentDocumentCurrentView> findAllCurrentByVersionIds(
            List<UUID> consentDocumentVersionIds,
            LocalDateTime now
    );
}
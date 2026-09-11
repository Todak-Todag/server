package com.todak_todag.user_service.user.domain.repository.query;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    // 약관 버전 상세 조회
    Optional<ConsentDocumentDetailView> findDetailByVersionId(
            UUID consentDocumentVersionId
    );

    // 논리 삭제되지 않은 약관 문서 조회
    Optional<ConsentDocument> findById(UUID consentDocumentId);

    // 삭제 여부를 포함한 약관 문서 조회
    Optional<ConsentDocument> findByIdIncludingDeleted(
            UUID consentDocumentId
    );

    // 사용 중인 동일 유형 약관 존재 여부
    boolean existsByConsentType(
            ConsentDocument.ConsentType consentType
    );

    // 동일 약관 내 버전 중복 여부 확인
    boolean existsVersion(
            UUID consentDocumentId,
            String version
    );

    // 필수 약관 버전 아이디 조회
    Optional<Boolean> findRequiredByVersionId(UUID consentDocumentVersionId);
}
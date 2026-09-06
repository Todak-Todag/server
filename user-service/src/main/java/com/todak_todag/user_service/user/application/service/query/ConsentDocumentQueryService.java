package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindDetailResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentDocumentQueryService {

    private final ConsentDocumentQueryRepository consentDocumentQueryRepository;

    public List<ConsentDocumentFindResult> findCurrentConsentDocuments() {
        LocalDateTime now = LocalDateTime.now();

        // 현재 적용 중인 약관 목록 조회
        return consentDocumentQueryRepository.findAllCurrent(now)
                .stream()
                .map(ConsentDocumentFindResult::from)
                .toList();
    }

    // 회원가입 시 전달된 약관 버전의 현재 유효 여부 조회
    public List<ConsentDocumentFindResult> findCurrentConsentDocumentsByVersionIds(
            List<UUID> consentDocumentVersionIds
    ) {
        if (consentDocumentVersionIds == null
                || consentDocumentVersionIds.isEmpty()) {
            return List.of();
        }

        List<UUID> uniqueVersionIds = consentDocumentVersionIds.stream()
                .distinct()
                .toList();

        LocalDateTime now = LocalDateTime.now();

        return consentDocumentQueryRepository
                .findAllCurrentByVersionIds(uniqueVersionIds, now)
                .stream()
                .map(ConsentDocumentFindResult::from)
                .toList();
    }

    // 약관 버전 상세 조회
    public ConsentDocumentFindDetailResult findConsentDocumentDetail(
            UUID consentDocumentVersionId
    ) {
        return consentDocumentQueryRepository
                .findDetailByVersionId(consentDocumentVersionId)
                .map(ConsentDocumentFindDetailResult::from)
                .orElseThrow(() ->
                        new BusinessException(
                                ConsentDocumentErrorCode.CONSENT_DOCUMENT_NOT_FOUND
                        )
                );
    }
}
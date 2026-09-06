package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentDocumentUpdateRequiredCommand;
import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentDocumentCommandService {

    private final ConsentDocumentQueryRepository consentDocumentQueryRepository;

    @Transactional
    public ConsentDocumentUpdateRequiredResult updateRequired(
            ConsentDocumentUpdateRequiredCommand command
    ) {
        ConsentDocument consentDocument =
                consentDocumentQueryRepository
                        .findById(command.consentDocumentId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ConsentDocumentErrorCode
                                                .CONSENT_DOCUMENT_NOT_FOUND
                                )
                        );

        consentDocument.updateRequired(
                command.isRequired()
        );

        log.info(
                "[ConsentDocument] 약관 필수 여부 변경 consentDocumentId={} isRequired={}",
                consentDocument.getId(),
                command.isRequired()
        );

        return ConsentDocumentUpdateRequiredResult.from(
                consentDocument
        );
    }
}
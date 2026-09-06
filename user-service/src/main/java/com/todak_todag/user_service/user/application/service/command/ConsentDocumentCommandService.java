package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentDocumentCreateCommand;
import com.todak_todag.user_service.user.application.command.ConsentDocumentDeleteCommand;
import com.todak_todag.user_service.user.application.command.ConsentDocumentUpdateRequiredCommand;
import com.todak_todag.user_service.user.application.result.ConsentDocumentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentVersionCommandRepository;
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
    private final ConsentDocumentCommandRepository consentDocumentCommandRepository;
    private final ConsentDocumentVersionCommandRepository
            consentDocumentVersionCommandRepository;

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

    // 약관 삭제 메서드
    @Transactional
    public void delete(
            ConsentDocumentDeleteCommand command
    ) {
        ConsentDocument consentDocument =
                consentDocumentQueryRepository
                        .findByIdIncludingDeleted(
                                command.consentDocumentId()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ConsentDocumentErrorCode
                                                .CONSENT_DOCUMENT_NOT_FOUND
                                )
                        );

        if (consentDocument.isDeleted()) {
            throw new BusinessException(
                    ConsentDocumentErrorCode
                            .CONSENT_DOCUMENT_ALREADY_DELETED
            );
        }

        consentDocument.markDeleted(
                command.deletedBy()
        );

        log.info(
                "[ConsentDocument] 약관 사용 종료 consentDocumentId={}",
                consentDocument.getId()
        );
    }

    @Transactional
    public ConsentDocumentCreateResult create(
            ConsentDocumentCreateCommand command
    ) {

        // 동일 유형의 사용 중인 약관은 하나만 존재할 수 있다.
        if (consentDocumentQueryRepository.existsByConsentType(
                command.consentType()
        )) {
            throw new BusinessException(
                    ConsentDocumentErrorCode
                            .CONSENT_DOCUMENT_ALREADY_EXISTS
            );
        }

        ConsentDocument consentDocument =
                ConsentDocument.create(
                        command.consentType(),
                        command.title(),
                        command.isRequired()
                );

        ConsentDocument savedDocument =
                consentDocumentCommandRepository.save(
                        consentDocument
                );

        ConsentDocumentVersion consentDocumentVersion =
                ConsentDocumentVersion.create(
                        savedDocument.getId(),
                        command.version(),
                        command.content(),
                        command.effectiveAt()
                );

        ConsentDocumentVersion savedVersion =
                consentDocumentVersionCommandRepository.save(
                        consentDocumentVersion
                );

        log.info(
                "[ConsentDocument] 약관 등록 consentDocumentId={} consentDocumentVersionId={} consentType={}",
                savedDocument.getId(),
                savedVersion.getId(),
                savedDocument.getConsentType()
        );

        return ConsentDocumentCreateResult.of(
                savedDocument,
                savedVersion
        );
    }
}
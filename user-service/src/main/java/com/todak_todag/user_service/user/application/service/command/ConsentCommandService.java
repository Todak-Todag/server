package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentCreateCommand;
import com.todak_todag.user_service.user.application.command.ConsentWithdrawCommand;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentWithdrawResult;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus.WITHDRAWN;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentCommandService {

    private final ConsentDocumentQueryRepository
            consentDocumentQueryRepository;

    private final ConsentQueryRepository
            consentQueryRepository;

    private final ConsentCommandRepository
            consentCommandRepository;

    private final UserQueryRepository
            userQueryRepository;

    @Transactional
    public ConsentCreateResult create(
            ConsentCreateCommand command
    ) {
        validateDuplicateVersionIds(command);

        LocalDateTime now = LocalDateTime.now();

        List<ConsentDocumentCurrentView> currentVersions =
                consentDocumentQueryRepository
                        .findAllCurrentByVersionIds(
                                command.consentDocumentVersionIds(),
                                now
                        );

        validateCurrentVersions(
                command,
                currentVersions
        );

        validateAlreadyAgreed(command);

        List<Consent> consents =
                command.consentDocumentVersionIds()
                        .stream()
                        .map(consentDocumentVersionId ->
                                Consent.agree(
                                        command.userId(),
                                        consentDocumentVersionId,
                                        now
                                )
                        )
                        .toList();

        List<Consent> savedConsents =
                consentCommandRepository.saveAll(
                        consents
                );

        approvePatientIfAllRequiredConsentsAgreed(
                command.userId(),
                now
        );

        log.info(
                "[Consent] 약관 동의 완료 userId={} consentCount={}",
                command.userId(),
                savedConsents.size()
        );

        return ConsentCreateResult.from(
                savedConsents
        );
    }

    private void validateDuplicateVersionIds(
            ConsentCreateCommand command
    ) {
        Set<UUID> uniqueVersionIds =
                new HashSet<>(
                        command.consentDocumentVersionIds()
                );

        if (uniqueVersionIds.size()
                != command.consentDocumentVersionIds().size()) {

            throw new BusinessException(
                    ConsentErrorCode
                            .DUPLICATE_CONSENT_DOCUMENT_VERSION
            );
        }
    }

    private void validateCurrentVersions(
            ConsentCreateCommand command,
            List<ConsentDocumentCurrentView> currentVersions
    ) {
        Set<UUID> requestedVersionIds =
                new HashSet<>(
                        command.consentDocumentVersionIds()
                );

        Set<UUID> currentVersionIds =
                currentVersions.stream()
                        .map(
                                ConsentDocumentCurrentView
                                        ::consentDocumentVersionId
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        if (!currentVersionIds.equals(
                requestedVersionIds
        )) {
            throw new BusinessException(
                    ConsentErrorCode
                            .INVALID_CONSENT_DOCUMENT_VERSION
            );
        }
    }

    private void validateAlreadyAgreed(
            ConsentCreateCommand command
    ) {
        if (consentQueryRepository.existsAgreedConsent(
                command.userId(),
                command.consentDocumentVersionIds()
        )) {
            throw new BusinessException(
                    ConsentErrorCode
                            .CONSENT_ALREADY_AGREED
            );
        }
    }

    @Transactional
    public ConsentWithdrawResult withdraw(
            ConsentWithdrawCommand command
    ) {
        Consent consent =
                consentCommandRepository
                        .findById(command.consentId())
                        .orElseThrow(
                                () -> new BusinessException(
                                        ConsentErrorCode.CONSENT_NOT_FOUND
                                )
                        );

        validateConsentOwner(
                consent,
                command.userId()
        );

        validateNotWithdrawn(consent);

        LocalDateTime now =
                LocalDateTime.now();

        consent.withdraw(now);

        log.info(
                "[Consent] 약관 동의 철회 완료 userId={} consentId={}",
                command.userId(),
                command.consentId()
        );

        return new ConsentWithdrawResult(
                command.consentId(),
                consent.getStatus(),
                consent.getWithdrawnAt()
        );
    }

    private void validateConsentOwner(
            Consent consent,
            UUID userId
    ) {
        if (!consent.getUserId().equals(userId)) {
            throw new BusinessException(
                    ConsentErrorCode.CONSENT_ACCESS_DENIED
            );
        }
    }

    private void validateNotWithdrawn(
            Consent consent
    ) {
        if (consent.getStatus() == WITHDRAWN) {
            throw new BusinessException(
                    ConsentErrorCode.CONSENT_ALREADY_WITHDRAWN
            );
        }
    }

    private void approvePatientIfAllRequiredConsentsAgreed(
            UUID userId,
            LocalDateTime now
    ) {
        User user = userQueryRepository
                .findById(userId)
                .orElseThrow(
                        () -> new BusinessException(
                                UserErrorCode.USER_NOT_FOUND
                        )
                );

        if (!user.isPatient() || !user.isWithdrawn()) {
            return;
        }

        List<UUID> requiredVersionIds =
                consentDocumentQueryRepository
                        .findAllCurrent(now)
                        .stream()
                        .filter(
                                ConsentDocumentCurrentView::isRequired
                        )
                        .map(
                                ConsentDocumentCurrentView
                                        ::consentDocumentVersionId
                        )
                        .toList();

        if (requiredVersionIds.isEmpty()) {
            return;
        }

        long agreedCount =
                consentQueryRepository
                        .countAgreedConsents(
                                userId,
                                requiredVersionIds
                        );

        if (agreedCount == requiredVersionIds.size()) {
            user.approveFromRequiredConsent();
        }
    }
}
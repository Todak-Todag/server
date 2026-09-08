package com.todak_todag.user_service.user.application.support;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsentDocumentValidator {

	private final ConsentDocumentQueryRepository consentDocumentQueryRepo;
	
	public Set<UUID> signupConsentDocumentValidate(UserSignupCommand signup) {
		Set<UUID> requestTermsIds = new HashSet<>(signup.getTermsIds());
		if(requestTermsIds.size() != signup.getTermsIds().size()) {
			throw new BusinessException(ConsentErrorCode.DUPLICATE_CONSENT_DOCUMENT_VERSION);
		}
				
		// 연산 빠르게 Map<UUID, ConsentDocumentCurrentView> 매핑
		Map<UUID, ConsentDocumentCurrentView> currentDocumentVersion = consentDocumentQueryRepo.findAllCurrent(LocalDateTime.now())
				.stream()
				.collect(Collectors.toMap(ConsentDocumentCurrentView::consentDocumentVersionId, version -> version));
		
		// agreements.agreed 가 true 로 넘어온 termsId 를 추출
		Set<UUID> agreedVersionIds = signup.agreements().stream()
				.filter(UserSignupCommand.AgreementCommand::agreed)
				.map(UserSignupCommand.AgreementCommand::termsId)
				.collect(Collectors.toSet());
		
		if(!currentDocumentVersion.keySet().containsAll(agreedVersionIds)) {
			throw new BusinessException(ConsentErrorCode.INVALID_CONSENT_DOCUMENT_VERSION);
		}
		
		Set<UUID> requiredVersionIds = currentDocumentVersion.values().stream()
				.filter(ConsentDocumentCurrentView::isRequired)
				.map(ConsentDocumentCurrentView::consentDocumentVersionId)
				.collect(Collectors.toSet());
		
		if(!agreedVersionIds.containsAll(requiredVersionIds)) {
			throw new BusinessException(UserErrorCode.USER_SIGNUP_REQUIRED_NOT_AGREED);
		}
		
		return agreedVersionIds;
	}
	
}

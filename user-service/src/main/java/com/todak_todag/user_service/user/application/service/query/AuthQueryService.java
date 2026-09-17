package com.todak_todag.user_service.user.application.service.query;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.support.MaskingUtil;
import com.todak_todag.user_service.user.application.result.AuthLoginSnapshotResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {

	private final UserQueryRepository userQueryRepo;
	
	private final ConsentQueryRepository consentQueryRepo;
	
	public AuthLoginSnapshotResult findLoginSnapshot(String username) {
		User user = userQueryRepo.findLoginByUsername(username)
				.orElseThrow(() -> {
					log.warn(
							"[User] 존재하지 않는 아이디로 로그인이 시도되었습니다. username={}",
							MaskingUtil.maskUsername(username)
					);
					
					return new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED);
				});
		
		user.validateCanLogin();
		
		return new AuthLoginSnapshotResult(
				user.getId(),
				user.getPasswordHash(),
				user.getRole(),
				user.isWithdrawn(),
				user.isPatientConsent()
		);
	}
	
	public boolean hasConsentHistory(UUID userId) {
		return !consentQueryRepo.findAllByUserId(userId).isEmpty();
	}
}

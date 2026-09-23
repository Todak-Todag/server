package com.todak_todag.user_service.user.application.facade;

import java.time.Duration;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthLoginCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.result.AuthLoginResult;
import com.todak_todag.user_service.user.application.result.AuthLoginSnapshotResult;
import com.todak_todag.user_service.user.application.service.command.AuthCommandService;
import com.todak_todag.user_service.user.application.service.query.AuthQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFacade {

	private final AuthQueryService authQueryService;
  
	private final AuthCommandService authCommandService;
  
	private final PasswordEncoderPort passwordEncoder;
  
	private final TokenStorePort tokenStorePort;
  
	private final TokenPort tokenPort;
	
	public AuthLoginResult login(AuthLoginCommand command) {
		// 1. 읽기 트랜잭션
		AuthLoginSnapshotResult snapshot = authQueryService.findLoginSnapshot(command.username());
		
		// 2. BCrypt 패스워드 일치 검증
		if(!passwordEncoder.matches(command.password(), snapshot.passwordHash())) {
			log.warn(
					"[User] 비밀번호가 일치하지 않는 로그인이 시도되었습니다. userId={}",
					snapshot.userId()
			);
			
			throw new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED);
		}
		
		// 3. WITHDRAWN 분기
		if(snapshot.withdrawn()) {
			if(snapshot.patientConsent() && !authQueryService.hasConsentHistory(snapshot.userId())) {
				log.info(
						"[User] 퇴원 예정자 첫 로그인으로 3분 임시 토큰을 발급합니다. userId={}",
						snapshot.userId()
				);
				
				String accessToken = tokenPort.createToken();
				String jwtAccessToken = tokenPort.createJwtAccessToken(snapshot.userId(), snapshot.role());
				String refreshToken = tokenPort.createToken();
				
				tokenStorePort.storeAccessTokenTemp(
						snapshot.userId(),
						accessToken,
						jwtAccessToken,
						Duration.ofMinutes(3)
				);
				
				return new AuthLoginResult(snapshot.userId(), accessToken, refreshToken);
			}
			
			log.info(
					"[User] 탈퇴한 계정으로 로그인이 시도되었습니다. userId={}",
					snapshot.userId()
			);
			
			throw new BusinessException(UserErrorCode.USER_LOGIN_WITHDRAWN);
		}
		
		// 4. 정상 로그인
		String accessToken = tokenPort.createToken();
		String jwtAccessToken = tokenPort.createJwtAccessToken(snapshot.userId(), snapshot.role());
		String refreshToken = tokenPort.createToken();

		return completeLoginWithRetry(snapshot.userId(), accessToken, jwtAccessToken, refreshToken);
	}

	// 동시 로그인으로 세션 생성이 유니크 제약(ux_p_auths_user_active)에 걸리면
	// completeLogin의 트랜잭션 전체가 롤백되며 예외가 여기까지 올라온다.
	// PostgreSQL은 실패한 트랜잭션을 이어서 쓸 수 없으므로, 완전히 새 트랜잭션으로 한 번 더 시도한다.
	// 재시도 시점엔 먼저 이긴 요청의 세션이 이미 커밋되어 있어 renew 경로로 정상 처리된다.
	private AuthLoginResult completeLoginWithRetry(
			UUID userId, String accessToken, String jwtAccessToken, String refreshToken
	) {
		try {
			return authCommandService.completeLogin(userId, accessToken, jwtAccessToken, refreshToken);
		} catch (DataIntegrityViolationException e) {
			log.info("[User] 동시 로그인으로 세션 생성이 충돌해 재시도합니다. userId={}", userId);

			return authCommandService.completeLogin(userId, accessToken, jwtAccessToken, refreshToken);
		}
	}
}

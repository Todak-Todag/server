package com.todak_todag.user_service.user.application.facade;

import java.time.Duration;

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
		
		return authCommandService.completeLogin(snapshot.userId(), accessToken, jwtAccessToken, refreshToken);
	}
}

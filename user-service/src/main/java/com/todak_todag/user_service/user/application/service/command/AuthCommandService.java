package com.todak_todag.user_service.user.application.service.command;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.AuthErrorCode;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthLoginCommand;
import com.todak_todag.user_service.user.application.command.AuthLogoutCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.result.AuthLoginResult;
import com.todak_todag.user_service.user.application.result.AuthReissueResult;
import com.todak_todag.user_service.user.application.support.TokenValidator;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentHistoryView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AuthCommandService {
	
	private final Duration refreshExpiration;
	
	private final TokenValidator tokenValidator;
	
	private final TokenStorePort tokenStorePort;

	private final TokenPort tokenPort;
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final AuthCommandRepository authCommandRepo;
	
	private final AuthQueryRepository authQueryRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	private final ConsentQueryRepository consentQueryRepo;
	
	public AuthCommandService(
			@Value("${jwt.refresh.expiration}") Duration refreshExpiration,
			TokenValidator tokenValidator,
			TokenStorePort tokenStorePort,
			TokenPort tokenPort,
			PasswordEncoderPort passwordEncoder,
			AuthCommandRepository authCommandRepo,
			AuthQueryRepository authQueryRepo,
			UserQueryRepository userQueryRepo,
			ConsentQueryRepository consentQueryRepo
	) {
		if(refreshExpiration == null) {
			log.error("[User] 서버 구동 실패 jwt.refresh.expiration 설정 값이 비어있습니다.");
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 jwt.refresh.expiration 설정 오류");
		}
		
		if(refreshExpiration.isNegative() || refreshExpiration.isZero()) {
			log.error("[User] 서버 구동 실패 jwt.refresh.expiration 설정 값이 유효하지 않습니다.");
			
			throw new IllegalArgumentException("[User] 서버 구동 실패 jwt.refresh.expiration 설정 오류");
		}
		
		this.tokenValidator = tokenValidator;
		this.refreshExpiration = refreshExpiration;
		this.tokenStorePort = tokenStorePort;
		this.tokenPort = tokenPort;
		this.passwordEncoder = passwordEncoder;
		this.authCommandRepo = authCommandRepo;
		this.authQueryRepo = authQueryRepo;
		this.userQueryRepo = userQueryRepo;
		this.consentQueryRepo = consentQueryRepo;
	}
	
	public AuthReissueResult reissue(String refreshToken) {
		// 1. 리프레시 토큰 검증
		tokenValidator.validateRefreshTokenCookie(refreshToken);
		
		// 2. 리프레시 토큰 해시
		String refreshTokenHash = tokenPort.hashToken(refreshToken);
		
		// 3. 리프레시 토큰 해시로 조회
		Auth loginSession = authQueryRepo.findActiveByRefreshTokenHash(refreshTokenHash)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));
		
		// 4. 만료 검증
		LocalDateTime now = LocalDateTime.now();
		
		loginSession.validateExpiration(now);
		
		// 5. 세션 소유자 조회 -> 계정 상태 조회.. 조회 되면 Approved 상태이며 삭제되지 않은 것
		User user = userQueryRepo.findActiveById(loginSession.getUserId())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		
		// 6. 새로운 토큰 발급
		String newAccessToken = tokenPort.createToken();
		String newJwtAccessToken = tokenPort.createJwtAccessToken(user.getId(), user.getRole());
		String newRefreshToken = tokenPort.createToken();
		
		// 7. 토큰 회전
		String newRefreshTokenHash = tokenPort.hashToken(newRefreshToken);
		
		loginSession.renew(newRefreshTokenHash, now.plus(refreshExpiration));
		
		// 8. Redis 저장
		tokenStorePort.storeAccessToken(newAccessToken, newJwtAccessToken);
		
		return new AuthReissueResult(newAccessToken, newRefreshToken);
	}
	
	public void logout(AuthLogoutCommand command) {
		Auth auth = authQueryRepo.findActiveByUserId(command.requesterId())
				.orElse(null);
		
		if(auth != null) {
			auth.logout();			
		}
		
		if(command.accessToken() != null && !command.accessToken().isBlank()) {
			tokenStorePort.deleteAccessToken(command.accessToken());			
		}
	}
	
	public AuthLoginResult login(AuthLoginCommand loginCommand) {
		// 1. 사용자 있나?
		User loginUser = userQueryRepo.findLoginByUsername(loginCommand.username())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED));	
		
		// 2. 로그인이 가능한 상태인가?
		loginUser.validateCanLogin();
		
		// 3. 로그인 가능한 상태니까 아이디와 비밀번호 검증
		if(!passwordEncoder.matches(loginCommand.password(), loginUser.getPasswordHash())) {
			throw new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED);
		}
		
		// 4. 로그인은 가능한데 WITHDRAWN 상태인가? - 치명적인 버그 발견
		if(loginUser.isWithdrawn()) {

			// WITHDRAWN 이면서 PATIENT 이면 - 첫 로그인 시점일 가능성이 있다.
			if(loginUser.isPatient()) {
			
				// 동의했던 내역이 존재하면 첫 로그인 시점이 아닌 퇴원 예정자가 동의를 철회한 것이다.
				if(consentQueryRepo.findAllByUserId(loginUser.getId()).isEmpty()) {
					log.info("[User] 퇴원 예정자가 첫 로그인을 시작하였습니다. userId={}", loginUser.getId());
					String accessToken = tokenPort.createToken();
					
					String jwtAccessToken = tokenPort.createJwtAccessToken(loginUser.getId(), loginUser.getRole());
					
					String refreshToken = tokenPort.createToken();
					
					// 3분짜리 임시 토큰 발급
					tokenStorePort.storeAccessTokenTemp(accessToken, jwtAccessToken, Duration.ofMinutes(3));
					
					return new AuthLoginResult(loginUser.getId(), accessToken, refreshToken);
				}
			}
			throw new BusinessException(UserErrorCode.USER_LOGIN_WITHDRAWN);
		}
		
		// 5. 랜덤 액세스 토큰 발급
		String accessToken = tokenPort.createToken();
		
		// 6. JWT 형식의 액세스 토큰 발급
		String jwtAccessToken = tokenPort.createJwtAccessToken(loginUser.getId(), loginUser.getRole());
		
		// 7. 리프레시 토큰 발급
		String refreshToken = tokenPort.createToken();
		
		// 8. 현재 시간 구하기
		LocalDateTime now = LocalDateTime.now();
		
		// 9. 기존 활성 세션이 있으면 갱신, 없으면 새로 생성 - 멱등처리
		String refreshTokenHash = tokenPort.hashToken(refreshToken);

		Auth loginSession = authQueryRepo.findActiveByUserId(loginUser.getId())
				.map(existingSession -> {
					existingSession.renew(refreshTokenHash, now.plus(refreshExpiration));
					return existingSession;
				})
				.orElseGet(() -> authCommandRepo.save(
						Auth.login(loginUser.getId(), refreshTokenHash, now.plus(refreshExpiration), now)
				));

		// 10. 발급한 AccessToken을 Redis에 저장 (실패 시 트랜잭션 전체 롤백)
		tokenStorePort.storeAccessToken(accessToken, jwtAccessToken);

		return new AuthLoginResult(loginSession.getUserId(), accessToken, refreshToken);
	}
	
}

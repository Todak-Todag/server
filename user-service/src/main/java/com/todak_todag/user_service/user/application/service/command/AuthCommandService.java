package com.todak_todag.user_service.user.application.service.command;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.AuthErrorCode;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthLogoutCommand;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.result.AuthLoginResult;
import com.todak_todag.user_service.user.application.result.AuthReissueResult;
import com.todak_todag.user_service.user.application.support.TokenValidator;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthCommandService {
	
	private final Duration refreshExpiration;
	
	private final TokenValidator tokenValidator;
	
	private final TokenStorePort tokenStorePort;

	private final TokenPort tokenPort;

	private final AuthCommandRepository authCommandRepo;

	private final AuthQueryRepository authQueryRepo;

	private final UserQueryRepository userQueryRepo;

	public AuthCommandService(
			@Value("${jwt.refresh.expiration}") Duration refreshExpiration,
			TokenValidator tokenValidator,
			TokenStorePort tokenStorePort,
			TokenPort tokenPort,
			AuthCommandRepository authCommandRepo,
			AuthQueryRepository authQueryRepo,
			UserQueryRepository userQueryRepo
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
		this.authCommandRepo = authCommandRepo;
		this.authQueryRepo = authQueryRepo;
		this.userQueryRepo = userQueryRepo;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public AuthReissueResult reissue(String refreshToken) {
		// 1. 리프레시 토큰 검증
		tokenValidator.validateRefreshTokenCookie(refreshToken);
		
		// 2. 리프레시 토큰 해시
		String refreshTokenHash = tokenPort.hashToken(refreshToken);
		
		// 3. 리프레시 토큰 해시로 조회
		Auth loginSession = authQueryRepo.findActiveByRefreshTokenHash(refreshTokenHash)
				.orElseThrow(() -> {
					// 이미 회전됐거나 로그아웃된 토큰의 재사용일 수 있어 탈취 정황으로 볼 여지가 있다.
					log.warn("[User] 유효한 세션이 없는 RefreshToken 으로 재발급이 시도되었습니다.");

					return new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);
				});

		// 4. 만료 검증
		LocalDateTime now = LocalDateTime.now();

		loginSession.validateExpiration(now);

		// 5. 세션 소유자 조회 -> 계정 상태 조회.. 조회 되면 Approved 상태이며 삭제되지 않은 것
		User user = userQueryRepo.findActiveById(loginSession.getUserId())
				.orElseThrow(() -> {
					// 활성 세션이 남아있는데 계정이 이용 불가 상태다. 정지·탈퇴 시 세션 정리가 누락됐다는 신호.
					log.warn(
							"[User] 활성 세션의 소유자가 이용 가능한 계정이 아닙니다. userId={}, authId={}",
							loginSession.getUserId(),
							loginSession.getId()
					);

					return new BusinessException(UserErrorCode.USER_NOT_FOUND);
				});
		
		// 6. 새로운 토큰 발급
		String newAccessToken = tokenPort.createToken();
		String newJwtAccessToken = tokenPort.createJwtAccessToken(user.getId(), user.getRole());
		String newRefreshToken = tokenPort.createToken();
		
		// 7. 토큰 회전
		String newRefreshTokenHash = tokenPort.hashToken(newRefreshToken);
		
		loginSession.renew(newRefreshTokenHash, now.plus(refreshExpiration));
		
		// 8. Redis 저장
		tokenStorePort.storeAccessToken(user.getId(), newAccessToken, newJwtAccessToken);

		log.info("[User] 토큰 재발급 완료 userId={}, authId={}", user.getId(), loginSession.getId());

		return new AuthReissueResult(newAccessToken, newRefreshToken);
	}

	@Transactional(rollbackFor = Exception.class)
	public void logout(AuthLogoutCommand command) {
		Auth auth = authQueryRepo.findActiveByUserId(command.requesterId())
				.orElse(null);

		if(auth != null) {
			auth.logout();
		} else {
			// 인증을 통과했는데 DB에 활성 세션이 없다. 세션 정합성이 깨진 상태.
			log.warn(
					"[User] 로그아웃 요청자의 활성 로그인 세션이 존재하지 않습니다. userId={}",
					command.requesterId()
			);
		}

		if(command.accessToken() != null && !command.accessToken().isBlank()) {
			tokenStorePort.deleteAccessToken(command.requesterId(), command.accessToken());
		}

		log.info("[User] 로그아웃 완료 userId={}", command.requesterId());
	}
	
	
	@Transactional(rollbackFor = Exception.class)
	public AuthLoginResult completeLogin(UUID userId, String accessToken, String jwtAccessToken, String refreshToken) {
		User user = userQueryRepo.findLoginById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		
		user.validateCanLogin();
		
		LocalDateTime now = LocalDateTime.now();
		String refreshTokenHash = tokenPort.hashToken(refreshToken);
		
		Auth loginSession = authQueryRepo.findActiveByUserId(user.getId())
				.map(existing -> {
					existing.renew(refreshTokenHash, now.plus(refreshExpiration));
					
					return existing;
				})
				.orElseGet(() -> authCommandRepo.save(Auth.login(
						user.getId(),
						refreshTokenHash,
						now.plus(refreshExpiration),
						now))
				);
		
		tokenStorePort.storeAccessToken(user.getId(), accessToken, jwtAccessToken);
		
		log.info(
				"[User] 로그인 완료 userId={}, role={}, authId={}",
				user.getId(),
				user.getRole(),
				loginSession.getId()
		);
		
		return new AuthLoginResult(loginSession.getUserId(), accessToken, refreshToken);
	}

}

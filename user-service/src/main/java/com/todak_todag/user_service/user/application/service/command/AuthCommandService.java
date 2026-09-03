package com.todak_todag.user_service.user.application.service.command;


import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthCommand.AuthLoginCommand;
import com.todak_todag.user_service.user.application.event.LoginSuccessEvent;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.result.AuthResult.AuthLoginResult;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AuthCommandService {
	
	private final ApplicationEventPublisher eventPublisher;
	
	private final TokenPort tokenPort;
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final AuthCommandRepository authCommandRepo;
	
	private final AuthQueryRepository authQueryRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	public AuthLoginResult login(AuthLoginCommand loginCommand) {
		// 1. 사용자 있나?
		User loginUser = userQueryRepo.findLoginByUsername(loginCommand.username())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));	
		
		// 2. 로그인이 가능한 상태인가?
		loginUser.validateCanLogin();
		
		// 3. 로그인은 가능한데 WITHDRAWN 상태인가?
		if(loginUser.isWithdrawn()) {
			// TODO: 동의서 약관 동의 페이지로 이동시키는 에러 응답 형식 작성 API 명세서 수정
		}
		
		// 4. 로그인 가능한 상태니까 아이디와 비밀번호 검증
		if(!passwordEncoder.matches(loginCommand.password(), loginUser.getPasswordHash())) {
			throw new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED);
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
					existingSession.renew(refreshTokenHash, now.plusDays(7), now);
					return existingSession;
				})
				.orElseGet(() -> authCommandRepo.save(
						Auth.login(loginUser.getId(), refreshTokenHash, now.plusDays(7), now)
				));

		// 10. 로그인 이벤트 생성
		LoginSuccessEvent loginEvent = new LoginSuccessEvent(
				accessToken,
				jwtAccessToken
		);
		
		// 11. 로그인 이벤트 발행
		eventPublisher.publishEvent(loginEvent);
		
		return new AuthLoginResult(loginSession.getUserId(), accessToken, refreshToken);
	}
	
}

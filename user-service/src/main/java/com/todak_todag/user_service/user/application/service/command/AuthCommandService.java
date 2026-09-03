package com.todak_todag.user_service.user.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthCommand.AuthLoginCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AuthCommandService {
	
	private final TokenPort tokenPort;
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final AuthCommandRepository authCommandRepo;
	
	private final AuthQueryRepository authQueryRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	public void login(AuthLoginCommand loginCommand) {
		
		// 1. 사용자 있나?
		User loginUser = userQueryRepo.findLoginByUsername(loginCommand.username())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));	
		
		// 2. 로그인이 가능한 상태인가?
		loginUser.validateCanLogin();
		
		// 3. 로그인은 가능한데 WITHDRAWN 상태인가?
		if(loginUser.isWithdrawn()) {
			// TODO: 동의서 약관 동의 페이지로 이동시키는 에러 응답 형식 작성
		}
		
		// 4. 로그인 가능한 상태니까 아이디와 비밀번호 검증
		if(!passwordEncoder.matches(loginCommand.password(), loginUser.getPasswordHash())) {
			throw new BusinessException(UserErrorCode.USER_LOGIN_MISMATCHED);
		}
		
		// 랜덤 액세스 토큰
		String accessToken = tokenPort.createAccessToken();
		
		// JWT 형식의 액세스 토큰
		String jwtAccessToken = tokenPort.createJwtAccessToken(null, null);
	}
	
}

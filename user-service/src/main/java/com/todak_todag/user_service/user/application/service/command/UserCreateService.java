package com.todak_todag.user_service.user.application.service.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserCommand.UserSignupCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserResult;
import com.todak_todag.user_service.user.application.result.UserResult.UserSignupCreatedResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

// User 생성 작업 담당 서비스
@Component
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserCreateService {
	
	private final PasswordEncoderPort passwordEncoder;
	
	private final UserCommandRepository userCommandRepo;
	
	private final UserQueryRepository userQueryRepo;
	
	// private final regionCommandRepo
	
	// private final regionQueryRepo
	
	// private final consentCommandRepo
	
	// private final consentQueryRepo
	
	public UserSignupCreatedResult createUserSignup(UserSignupCommand signup) {
		
		// 요청에 지역ID 존재하면 regionId 검증
		if(signup.regionId() != null) {
			// TODO: regionId 존재 검증
		}
		
		// Username 중복 검증 : 가벼운 작업 위로
		if(userQueryRepo.duplicateUsername(signup.username())) {
			throw new BusinessException(UserErrorCode.USER_DUPLICATE_USERNAME);
		}
		
		/* TODO: consent 검증
		 * agreements.termsId --> 존재하는 동의서 약관 전체
		 * 1. termsId 를 List<UUID> 로 만든다 -> signup.getTermsIds
		 * 2. In절을 이용하여 List<ConsentDocumentVersion> 조회
		 * 3. List<UUID>.size == List<ConsentDocumentVersion>.size
		 * 4. Map<UUID, Boolean> 으로 필수수락해야 할 동의서 약관 매핑
		 * 5. 필수 약관인 조건 Filter 활용해서 boolean 값 비교 nayMatch
		 * 6. 전부 통과하면 List<Consent> 로 만들기
		 */
		
		// 비밀번호 해시
		String passwordHash = passwordEncoder.encode(signup.password());
		
		// 회원가입용 User 팩토리 생성자
		User signupUser = User.createSignup(
				signup.regionId(),
				signup.username(),
				passwordHash,
				signup.name(),
				signup.phone(),
				UserRole.valueOf(signup.type())
		);
		
		User user = userCommandRepo.save(signupUser);
		// List<Consent> saveAll
		
		return new UserResult.UserSignupCreatedResult(user.getId(), user.getName());
	}
	
	public void createUserAdmin() {
		// TODO: 운영자 등록 API
	}
	
	public void createUserPatient() {
		// TODO: 퇴원 예정자 등록 API
	}
	
}

package com.todak_todag.user_service.user.application.service.command;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;
import com.todak_todag.user_service.user.application.command.UserSuspendCommand;
import com.todak_todag.user_service.user.application.result.UserApprovalResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class UserUpdateService {

	private UserCommandRepository userCommandRepo;
	
	private UserQueryRepository userQueryRepo;
	
	public UserApprovalResult approval(UserApprovalCommand command) {
		// 1. 요청자의 신원이 뭐니?
		UserRole requesterRole = command.requesterRole();
		
		// 2. 요청자가 운영자면 승인/거절 대상의 지역과 같은지 확인한다.
		
		// 2-1. 대상자 조회
		User target = userQueryRepo.findById(command.userId())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		
		if(Objects.equals(requesterRole, UserRole.ADMIN)) {
			
			// 2-1. 관리자를 조회한다. 없으면 권한이 없는 것이다.
			User admin = userQueryRepo.findAdminById(command.requesterId())
					.orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

			// 2-3. 지역이 다르면 권한이 없다.
			if(!Objects.equals(admin.getRegionId(), target.getRegionId())) {
				throw new BusinessException(CommonErrorCode.FORBIDDEN);
			}
		}
		
		// 3. 대상 유저에게 승인 또는 거절한다.
		target.approvalOrReject(command.accept(), command.rejectReason());
		
		return new UserApprovalResult(
				target.getId(),
				target.getRole(),
				target.getStatusChangeReason(),
				command.accept()
		);
	}
	
	public UUID suspend(UserSuspendCommand command) {
		// 1. 요청자의 신원이 뭐니?
		UserRole requesterRole = command.requesterRole();
		
		// 2. 정지 대상이 존재하는가?
		User user = userQueryRepo.findById(command.userId())
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
		
		// 3. 요청자가 ADMIN 인가?
		if(Objects.equals(requesterRole, UserRole.ADMIN)) {
			
			// 3-1. ADMIN 은 ADMIN 을 정지시킬 수 없다.
			if(Objects.equals(user.getRole(), UserRole.ADMIN)) {
				throw new BusinessException(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
			}
			
			// 3-2. ADMIN 은 같은 지역내 사용자만 정지가 가능하다.
			User requesterAdmin = userQueryRepo.findActiveById(command.requesterId())
					.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST));
			
			if(Objects.equals(user.getRegionId(), requesterAdmin.getRegionId())) {
				throw new BusinessException(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
			}
		}
		
		// 4. 3번 IF문을 안타면 MASTER 이며 일시 정지를 진행한다.
		if(!user.isApprove()) {
			throw new BusinessException(UserErrorCode.USER_SUSPEND_MODIFY_STATE);
		}
		
		user.suspend(command.suspendReason());
		
		return user.getId();
	}
	
}
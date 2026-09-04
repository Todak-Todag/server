package com.todak_todag.user_service.user.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;
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
	
}
package com.todak_todag.user_service.user.application.command;

import java.util.List;
import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

public final class UserCommand {

	public record UserSignupCommand(
			UserRole type,
			String username,
			String password,
			String name,
			String phone,
			UUID regionId,
			List<AgreementCommand> agreements
	) {
		public record AgreementCommand(UUID termsId, Boolean agreed) {}
		
		public List<UUID> getTermsIds() {
			return this.agreements.stream()
					.map(termsId -> termsId.termsId())
					.toList();
		}
	}
	
	public record UserAdminCreateCommand(
			String username,
			String password,
			String name,
			String phone,
			UUID regionId
	) {}
	
}

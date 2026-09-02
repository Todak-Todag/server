package com.todak_todag.user_service.user.application.command;

import java.util.List;
import java.util.UUID;

public final class UserCommand {

	public record UserSignupCreateCommand(
			String type,
			String username,
			String password,
			String name,
			String phone,
			UUID regionId,
			List<AgreementCommand> agreements
	) {
		record AgreementCommand(UUID termsId, Boolean agreed) {}
		
		public List<UUID> getTermsIds() {
			return this.agreements.stream()
					.map(termsId -> termsId.termsId())
					.toList();
		}
	}
	
	public record UserAdminCreateCommand() {}
	
}

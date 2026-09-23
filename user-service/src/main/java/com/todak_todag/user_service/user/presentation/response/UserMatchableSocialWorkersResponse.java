package com.todak_todag.user_service.user.presentation.response;

import java.util.Set;
import java.util.UUID;

public record UserMatchableSocialWorkersResponse(
		Set<UUID> socialWorkerIds
) {

}

package com.todak_todag.user_service.user.application.result;

import java.util.UUID;

public record UserPatientCreatedResult(
		UUID patientId,
		UUID hospitalStaffId,
		String name,
		String phone,
		UUID regionId
) {
	
}

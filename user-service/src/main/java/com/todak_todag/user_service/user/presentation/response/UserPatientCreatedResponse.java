package com.todak_todag.user_service.user.presentation.response;

import java.util.UUID;

public record UserPatientCreatedResponse(
		UUID patientId,
		UUID hospitalStaffId,
		String name,
		String phone,
		UUID regionId
) {

}

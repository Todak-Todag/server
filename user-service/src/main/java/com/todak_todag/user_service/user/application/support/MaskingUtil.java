package com.todak_todag.user_service.user.application.support;

public final class MaskingUtil {

	private static final String MASKED = "***";
	
	private MaskingUtil() {}
	
	public static String maskName(String name) {
		if(isBlank(name) || name.length() < 2) {
			return MASKED;
		}
		
		return name.charAt(0) + "*".repeat(name.length() - 1);
	}
	
	public static String maskUsername(String username) {
		if(isBlank(username) || username.length() < 4) {
			return MASKED;
		}
		
		return username.substring(0, 2) + "*".repeat(username.length() - 2);
	}
	
	public static String maskPhone(String phone) {
		if(isBlank(phone)) {
			return MASKED;
		}
		
		String digits = phone.replaceAll("[^0-9]", "");
		
		if(digits.length() < 10) {
			return MASKED;
		}
		
		return digits.substring(0, 3) + "*".repeat(digits.length() - 3);
	}
	
	public static String maskAddress(String address) {
		return isBlank(address) ? MASKED : MASKED;
	}
	
	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

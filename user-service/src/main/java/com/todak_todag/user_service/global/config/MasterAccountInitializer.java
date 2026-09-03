package com.todak_todag.user_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.service.command.UserCreateService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MasterAccountInitializer implements CommandLineRunner {

	private static final String MASTER_PHONE = "01000000000";

	private final String username;

	private final String password;

	private final String name;

	private final UserCreateService userCreateService;

	public MasterAccountInitializer(
			@Value("${master.username:}") String username,
			@Value("${master.password:}") String password,
			@Value("${master.name:}") String name,
			UserCreateService userCreateService
	) {
		this.username = username;
		this.password = password;
		this.name = name;
		this.userCreateService = userCreateService;
	}

	@Override
	public void run(String... args) {
		if(username.isBlank() || password.isBlank() || name.isBlank()) {
			log.warn("[User] 마스터 계정 설정 값(master.username/password/name)이 없어 초기화를 건너뜁니다.");
			return;
		}

		userCreateService.createUserMaster(username, password, name, MASTER_PHONE);

		log.info("[User] 마스터 계정 확인 완료 username={}", username);
	}

}

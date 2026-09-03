package com.todak_todag.user_service.user.application.port;

import com.todak_todag.user_service.user.application.event.LoginSuccessEvent;

public interface LoginEventListenerPort {

	void handleUserLogin(LoginSuccessEvent event);
}

package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.user.User;

public interface UserCommandRepository {

	User save(User user);

	// 유니크 제약(ux_p_users_username_active) 위반을 그 자리에서 잡아야 할 때 사용한다.
	// save()는 커밋 시점까지 INSERT를 미루므로 즉시 flush가 필요하다.
	User saveAndFlush(User user);

}

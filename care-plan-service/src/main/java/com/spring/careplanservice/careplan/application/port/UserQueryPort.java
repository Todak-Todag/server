package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.result.UserFindResult;

import java.util.UUID;

public interface UserQueryPort {
    UserFindResult findById(
            UUID userId
    );
}

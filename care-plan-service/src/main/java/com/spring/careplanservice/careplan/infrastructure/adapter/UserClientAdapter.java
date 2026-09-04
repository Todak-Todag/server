package com.spring.careplanservice.careplan.infrastructure.adapter;


import com.spring.careplanservice.careplan.application.port.UserQueryPort;
import com.spring.careplanservice.careplan.application.result.UserFindResult;
import com.spring.careplanservice.careplan.infrastructure.client.UserFeignClient;
import com.spring.careplanservice.careplan.infrastructure.client.UserInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientAdapter implements UserQueryPort {
    private final UserFeignClient userFeignClient;

    @Override
    public UserFindResult findById(
            UUID userId
    ) {
        UserInternalResponse userInternalResponse = userFeignClient.findById(
                userId
        );

        UserInternalResponse.Data data = userInternalResponse.data();

        return new UserFindResult(
                data.userId(),
                data.role(),
                data.regionId()
        );
    }
}

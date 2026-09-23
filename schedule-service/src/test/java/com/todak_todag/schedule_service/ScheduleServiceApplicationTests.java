package com.todak_todag.schedule_service;

import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ScheduleServiceApplicationTests extends PostgresTestSupport {

	@Test
	void contextLoads() {
	}

}

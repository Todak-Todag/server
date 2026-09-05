package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.security.InternalHeader;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceResultInternalControllerTest {

    private static final String URI = "/internal/v1/service-results";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Value("${internal.key}")
    private String internalKey;

    @AfterEach
    void tearDown() {
        springDataCarePlanServiceResultRepository.deleteAll();
    }

    @Test
    void 존재하는_serviceResultId로_정상_조회한다() throws Exception {
        // given
        UUID serviceScheduleId = UUID.randomUUID();
        LocalDateTime startedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime finishedAt = LocalDateTime.now().minusHours(1);
        CarePlanServiceResult saved = persist(serviceScheduleId, startedAt, finishedAt);

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", saved.getServiceResultId())
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceResultId").value(saved.getServiceResultId().toString()))
                .andExpect(jsonPath("$.data.serviceScheduleId").value(serviceScheduleId.toString()));
    }

    @Test
    void 존재하지_않는_serviceResultId면_404를_반환한다() throws Exception {
        // given
        UUID notExistingId = UUID.randomUUID();

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", notExistingId)
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_NOT_FOUND"));
    }

    @Test
    void 논리_삭제된_serviceResultId면_404를_반환한다() throws Exception {
        // given
        CarePlanServiceResult saved = persist(
                UUID.randomUUID(), LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)
        );
        saved.markDeleted(UUID.randomUUID());
        springDataCarePlanServiceResultRepository.save(saved);

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", saved.getServiceResultId())
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_NOT_FOUND"));
    }

    private CarePlanServiceResult persist(UUID serviceScheduleId, LocalDateTime startedAt, LocalDateTime finishedAt) {
        CarePlanServiceResult result = CarePlanServiceResult.record(serviceScheduleId, startedAt, finishedAt, "비고");
        return springDataCarePlanServiceResultRepository.save(result);
    }
}

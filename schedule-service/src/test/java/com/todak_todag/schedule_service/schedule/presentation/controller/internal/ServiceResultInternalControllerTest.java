package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.security.InternalHeader;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import com.todak_todag.schedule_service.support.PostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceResultInternalControllerTest extends PostgresTestSupport {

    private static final String URI = "/internal/v1/service-results";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Value("${internal.key}")
    private String internalKey;

    @AfterEach
    void tearDown() {
        springDataCarePlanServiceResultRepository.deleteAll();
        springDataServiceScheduleRepository.deleteAll();
    }

    @Test
    void 존재하는_serviceResultId로_조회하면_carePlanId와_serviceResultId를_반환한다() throws Exception {
        // given
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID());
        CarePlanServiceResult saved = persistResult(schedule);

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", saved.getServiceResultId())
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.carePlanId").value(schedule.getCarePlanId().toString()))
                .andExpect(jsonPath("$.data.serviceResultId").value(saved.getServiceResultId().toString()))
                .andExpect(jsonPath("$.data.serviceScheduleId").doesNotExist())
                .andExpect(jsonPath("$.data.startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.finishedAt").doesNotExist());
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
        CarePlanServiceResult saved = persistResult(persistSchedule(UUID.randomUUID()));
        saved.markDeleted(UUID.randomUUID());
        springDataCarePlanServiceResultRepository.save(saved);

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", saved.getServiceResultId())
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_NOT_FOUND"));
    }

    // carePlanId를 확보할 수 없으면 존재 검증 자체가 불가능하므로 결과 미존재와 동일하게 404
    @Test
    void 조인_대상_일정이_논리_삭제됐으면_404를_반환한다() throws Exception {
        // given
        ServiceSchedule schedule = persistSchedule(UUID.randomUUID());
        CarePlanServiceResult saved = persistResult(schedule);
        schedule.markDeleted(UUID.randomUUID());
        springDataServiceScheduleRepository.save(schedule);

        // when & then
        mockMvc.perform(get(URI + "/{serviceResultId}", saved.getServiceResultId())
                        .header(InternalHeader.INTERNAL_KEY, internalKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SERVICE_RESULTS_NOT_FOUND"));
    }

    private ServiceSchedule persistSchedule(UUID carePlanId) {
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceSchedule schedule = ServiceSchedule.confirm(
                carePlanId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        return springDataServiceScheduleRepository.save(schedule);
    }

    private CarePlanServiceResult persistResult(ServiceSchedule schedule) {
        CarePlanServiceResult result = CarePlanServiceResult.record(
                schedule.getId(),
                schedule.getStartedAt(),
                schedule.getFinishedAt(),
                "비고"
        );
        return springDataCarePlanServiceResultRepository.save(result);
    }
}

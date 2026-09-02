package com.todak_todag.schedule_service.schedule.presentation.controller.internal;

import com.todak_todag.schedule_service.global.common.SystemId;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceScheduleInternalControllerTest {

    private static final String URI = "/internal/v1/service-schedules";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @AfterEach
    void tearDown() {
        springDataServiceScheduleRepository.deleteAll();
    }

    @Test
    void 정상_조회시_SCHEDULED와_RESCHEDULING만_반환하고_나머지_상태는_제외한다() throws Exception {
        // given
        UUID serviceOfferingId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);

        persist(serviceOfferingId, startDate);

        ServiceSchedule rescheduling = persist(serviceOfferingId, startDate);
        rescheduling.rescheduling();
        springDataServiceScheduleRepository.save(rescheduling);

        ServiceSchedule changed = persist(serviceOfferingId, startDate);
        setStatus(changed, ScheduleStatus.CHANGED);
        springDataServiceScheduleRepository.save(changed);

        ServiceSchedule completed = persist(serviceOfferingId, startDate);
        completed.complete();
        springDataServiceScheduleRepository.save(completed);

        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", serviceOfferingId.toString())
                        .param("startDate", startDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void 여러_serviceOfferingIds를_콤마로_전달하면_모두_조회된다() throws Exception {
        // given
        UUID offeringA = UUID.randomUUID();
        UUID offeringB = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);

        persist(offeringA, startDate);
        persist(offeringB, startDate);

        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", offeringA + "," + offeringB)
                        .param("startDate", startDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void 조회_결과가_없으면_빈_배열을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", UUID.randomUUID().toString())
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void serviceOfferingIds가_없으면_400을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get(URI).param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void startDate가_없으면_400을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get(URI).param("serviceOfferingIds", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void startDate_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", UUID.randomUUID().toString())
                        .param("startDate", "2026/09/01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void serviceOfferingIds_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", "not-a-uuid")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void serviceOfferingIds가_빈_값이면_400을_반환한다() throws Exception {
        // ScheduleInternalApiSpec에 선언된 @NotEmpty가 Controller의 @Override 메서드에
        // 상속되어(HV000151 회피를 위해 재선언하지 않음) 정상 동작하는지 검증한다.
        // when & then
        mockMvc.perform(get(URI)
                        .param("serviceOfferingIds", "")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 인증_주체가_없는_요청으로_저장된_일정의_created_by는_SYSTEM_USER_ID다() {
        // given & when
        ServiceSchedule schedule = persist(UUID.randomUUID(), LocalDate.now().plusDays(1));

        // then
        assertThat(schedule.getCreatedBy()).isEqualTo(SystemId.SYSTEM_USER_ID);
    }

    private ServiceSchedule persist(UUID serviceOfferingId, LocalDate date) {
        ServiceSchedule schedule = ServiceSchedule.confirm(
                UUID.randomUUID(),
                serviceOfferingId,
                date,
                date.atTime(9, 0),
                date.atTime(10, 0)
        );
        return springDataServiceScheduleRepository.save(schedule);
    }

    private void setStatus(ServiceSchedule schedule, ScheduleStatus status) {
        try {
            Field statusField = ServiceSchedule.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(schedule, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

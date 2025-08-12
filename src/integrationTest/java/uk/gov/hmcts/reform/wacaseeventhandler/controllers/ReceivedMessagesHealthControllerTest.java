package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CASE_EVENT_HANDLER_MESSAGE_HEALTH;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.NO_MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.NO_MESSAGE_CHECK;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=false",
    "environment=test",
    "management.endpoint.health.receivedMessageCheckEnvEnabled=test"})
@Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_case_event_messages.sql"})
public class ReceivedMessagesHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private CaseEventMessageRepository caseEventMessageRepository;

    @SpyBean
    private HolidayService holidayService;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setup() {
        reset(caseEventMessageRepository);
    }

    @Test
    void test_health_reports_down_if_no_messages_received_in_last_hour_during_working_hours() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 16,15));

        //THEN
        assertReceivedMessagesHealthStatus(DOWN, NO_MESSAGES_RECEIVED);
    }

    @Test
    void test_health_reports_up_if_messages_received_in_last_hour_during_working_hours() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 12,15));

        // THEN
        assertReceivedMessagesHealthStatus(UP, MESSAGES_RECEIVED);
    }

    @Test
    void test_health_reports_up_if_no_messages_received_in_last_hour_during_weekend() throws Exception {
        // GIVEN
        LocalDateTime localDateTime = LocalDateTime.of(2022, 8, 28, 12, 15);
        setClock(localDateTime);

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(holidayService).isWeekend(localDateTime.toLocalDate());
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void should_health_reports_up_when_no_messages_received_in_last_hour_during_holiday() throws Exception {
        // GIVEN
        LocalDateTime localDateTime = LocalDateTime.of(2025, 8, 25, 12, 15);
        setClock(localDateTime);

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(holidayService).isHoliday(localDateTime.toLocalDate());
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void test_health_reports_up_if_time_outside_of_working_hours_start_time() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 7,29));

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void test_health_reports_up_if_time_outside_of_working_hours_end_time() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 18,31));

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:scripts/insert_case_event_messages_for_received_messages_check.sql"})
    void should_health_check_reports_up_when_messages_received_in_last_hour() throws Exception {

        // GIVEN
        setClock(LocalDateTime.of(2024, 4, 2, 14,15));

        // THEN
        mockMvc.perform(get("/health"))
            .andExpect(
                jsonPath("$.components.ccdMessagesReceived.status")
                    .value(UP.toString()))
            .andExpect(jsonPath("$.components.ccdMessagesReceived.details." + CASE_EVENT_HANDLER_MESSAGE_HEALTH)
                           .value(String.format(MESSAGES_RECEIVED, "test")));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:scripts/insert_case_event_messages_for_received_messages_check.sql"})
    void should_verify_health_check_reports_is_down_when_no_messages_received_in_last_hour() throws Exception {

        // GIVEN
        setClock(LocalDateTime.of(2024, 4, 2, 15,15));

        // THEN
        mockMvc.perform(get("/health"))
            .andExpect(
                jsonPath("$.components.ccdMessagesReceived.status")
                    .value(DOWN.toString()))
            .andExpect(jsonPath("$.components.ccdMessagesReceived.details." + CASE_EVENT_HANDLER_MESSAGE_HEALTH)
                           .value(String.format(NO_MESSAGES_RECEIVED, "test")));

    }

    @ParameterizedTest
    @MethodSource(value = "workingHoursWithTimeZoneScenarioProvider")
    void should_invoke_case_event_repo_when_time_is_within_working_hours(
        LocalDateTime withinWorkingHoursDate) throws Exception {

        // GIVEN
        setClock(withinWorkingHoursDate);

        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(10);

        // THEN
        assertReceivedMessagesHealthStatus(UP, MESSAGES_RECEIVED);

    }

    @ParameterizedTest
    @MethodSource(value = "nonWorkingHoursForDstTimeZoneStartTimeAndEndTime")
    void should_not_invoke_case_event_repo_when_time_is_outside_working_hours(
        LocalDateTime outsideWorkingHoursDate) throws Exception {

        // GIVEN
        setClock(outsideWorkingHoursDate);

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    private static Stream<LocalDateTime> workingHoursWithTimeZoneScenarioProvider() {
        return Stream.of(
            LocalDateTime.of(2024, Month.JANUARY, 02, 10, 00),
            LocalDateTime.of(2024, Month.MAY, 01, 9, 00)
        );
    }

    private static Stream<LocalDateTime> nonWorkingHoursForDstTimeZoneStartTimeAndEndTime() {
        return Stream.of(
            LocalDateTime.of(2024, Month.OCTOBER, 27, 07, 00),
            LocalDateTime.of(2024, Month.MARCH, 31, 17, 00)
        );
    }

    private void assertReceivedMessagesHealthStatus(Status status, String details) throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(
                jsonPath("$.components.ccdMessagesReceived.status")
                    .value(status.toString()))
            .andExpect(jsonPath("$.components.ccdMessagesReceived.details." + CASE_EVENT_HANDLER_MESSAGE_HEALTH)
                           .value(details));
    }

    private void setClock(LocalDateTime localDateTime) {
        when(clock.instant()).thenReturn(localDateTime.toInstant(ZoneOffset.UTC));
        ZoneId zoneId = ZoneId.of("UTC");
        when(clock.getZone()).thenReturn(zoneId);
    }
}

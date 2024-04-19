package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CASE_EVENT_HANDLER_MESSAGE_HEALTH;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CHECK_DISABLED_MESSAGE;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.NO_MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.NO_MESSAGE_CHECK;

@ExtendWith(MockitoExtension.class)
class ReceivedMessagesHealthControllerTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private Clock clock;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private ReceivedMessagesHealthController receivedMessagesHealthController;

    @BeforeEach
    void setup() {
        setField(receivedMessagesHealthController, "receivedMessageCheckEnvEnabled", "validEnvironment,test");
        setField(receivedMessagesHealthController, "environment", "validEnvironment");
    }

    @Test
    void test_health_reports_error_if_no_messages_received_in_last_hour() {
        // GIVEN
        setupDefaultMockClock();
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(0);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(DOWN, health.getStatus());
        assertEquals(NO_MESSAGES_RECEIVED, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @Test
    void test_health_reports_success_if_messages_received_in_last_hour() {
        // GIVEN
        setupDefaultMockClock();
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(1);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(MESSAGES_RECEIVED, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @Test
    void test_health_does_not_call_repository_if_non_working_day_holiday() {
        // GIVEN
        setupDefaultMockClock();
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(true);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(NO_MESSAGE_CHECK, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void test_health_does_not_call_repository_if_non_working_day_weekend() {
        // GIVEN
        setupDefaultMockClock();
        when(holidayService.isWeekend(any(LocalDate.class))).thenReturn(true);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(NO_MESSAGE_CHECK, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @ParameterizedTest
    @MethodSource(value = "nonWorkingHoursScenarioProvider")
    void test_health_does_not_call_repository_if_working_day_time_is_out_of_working_hours(
        LocalDateTime outOfWorkingHoursDate) {
        // GIVEN
        setupMockClock(outOfWorkingHoursDate);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(NO_MESSAGE_CHECK, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(outOfWorkingHoursDate);
    }

    @ParameterizedTest
    @MethodSource(value = "workingHoursScenarioProvider")
    void test_health_calls_repository_if_working_day_time_is_within_working_hours(
        LocalDateTime withinWorkingHoursDate) {
        // GIVEN
        setupMockClock(withinWorkingHoursDate);

        receivedMessagesHealthController.health();

        verify(caseEventMessageRepository).getNumberOfMessagesReceivedInLastHour(withinWorkingHoursDate.plusHours(1));
    }

    @ParameterizedTest
    @MethodSource(value = "workingHoursWithTimeZoneScenarioProvider")
    void test_health_calls_repository_if_working_day_time_is_within_working_hours_with_timezone(
        LocalDateTime withinWorkingHoursDate) {
        // GIVEN
        setupMockClock(withinWorkingHoursDate);
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(10);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(MESSAGES_RECEIVED, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @ParameterizedTest
    @MethodSource(value = "nonWorkingHoursForDstTimeZoneStartTimeAndEndTime")
    void test_health_calls_repository_if_working_day_time_is_outside_working_hours_with_timezone(
        LocalDateTime outsideWorkingHoursDate) {
        // GIVEN
        setupMockClock(outsideWorkingHoursDate);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(NO_MESSAGE_CHECK, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @ParameterizedTest
    @MethodSource(value = "nonWorkingHoursForDstTimeZoneStartTimeAndEndTime")
    void test_health_reports_success_if_messages_check_disabled_in_current_environment() {
        // GIVEN
        setField(receivedMessagesHealthController, "environment", "invalidEnvironment");
        setField(receivedMessagesHealthController, "receivedMessageCheckEnvEnabled", "validEnvironment,test");

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(String.format(CHECK_DISABLED_MESSAGE, "invalidEnvironment"),
                     health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
        verifyNoInteractions(clock);
        verifyNoInteractions(holidayService);
        verifyNoInteractions(caseEventMessageRepository);
    }

    @ParameterizedTest
    @MethodSource(value = "enabledEnvProvider")
    void test_enabled_for_environment_return_false_prod_aat(String env) {

        setField(receivedMessagesHealthController, "receivedMessageCheckEnvEnabled", "prod,aat");

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setServerName("case-event-handler.any.environment");
        mockHttpServletRequest.setRequestURI("/health");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockHttpServletRequest));

        assertFalse(receivedMessagesHealthController.isNotEnabledForEnvironment(env));
    }

    @ParameterizedTest
    @MethodSource(value = "disabledEnvProvider")
    void test_enabled_for_environment_return_true_demo_ithc(String env) {

        setField(receivedMessagesHealthController, "receivedMessageCheckEnvEnabled", "prod,aat");

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setServerName("case-event-handler.any.environment");
        mockHttpServletRequest.setRequestURI("/health");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockHttpServletRequest));

        assertTrue(receivedMessagesHealthController.isNotEnabledForEnvironment(env));
    }

    @Test
    void test_enabled_for_environment_return_true_staging_aat() {

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setServerName("case-event-handler.staging.aat");
        mockHttpServletRequest.setRequestURI("/health");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockHttpServletRequest));

        assertTrue(receivedMessagesHealthController.isNotEnabledForEnvironment("aat"));
    }

    private void setupDefaultMockClock() {
        setupMockClock(LocalDateTime.of(2022, Month.SEPTEMBER, 01, 11, 30));
    }

    private void setupMockClock(LocalDateTime localDateTime) {
        Clock fixedClock = Clock.fixed(localDateTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    private static Stream<LocalDateTime> nonWorkingHoursScenarioProvider() {
        return Stream.of(
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 7, 29),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 18, 31),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 0, 0)

        );
    }

    private static Stream<LocalDateTime> workingHoursScenarioProvider() {
        return Stream.of(
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 8, 30),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 8, 31),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 16, 29),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 16, 30)
        );
    }

    private static Stream<LocalDateTime> workingHoursWithTimeZoneScenarioProvider() {
        return Stream.of(
            LocalDateTime.of(2024, Month.JANUARY, 01, 10, 00),
            LocalDateTime.of(2024, Month.MAY, 01, 9, 00)
        );
    }

    private static Stream<LocalDateTime> nonWorkingHoursForDstTimeZoneStartTimeAndEndTime() {
        return Stream.of(
            LocalDateTime.of(2024, Month.OCTOBER, 27, 07, 00),
            LocalDateTime.of(2024, Month.MARCH, 31, 17, 00)
        );
    }

    private static Stream<String> enabledEnvProvider() {
        return Stream.of(
            "prod","aat"
        );
    }

    private static Stream<String> disabledEnvProvider() {
        return Stream.of(
            "demo","ithc"
        );
    }
}

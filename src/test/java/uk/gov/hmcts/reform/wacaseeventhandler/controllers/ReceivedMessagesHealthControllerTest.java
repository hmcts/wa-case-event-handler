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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CASE_EVENT_HANDLER_MESSAGE_HEALTH;
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
        setupDefaultMockClock();
    }

    @Test
    void testHealthReportsErrorIfNoMessagesReceivedInLastHour() {
        // GIVEN
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(0);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(DOWN, health.getStatus());
        assertEquals(NO_MESSAGES_RECEIVED, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @Test
    void testHealthReportsSuccessIfMessagesReceivedInLastHour() {
        // GIVEN
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(1);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(MESSAGES_RECEIVED, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
    }

    @Test
    void testHealthDoesNotCallRepositoryIfNonWorkingDayHoliday() {
        // GIVEN
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(true);

        // WHEN
        Health health = receivedMessagesHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        assertEquals(NO_MESSAGE_CHECK, health.getDetails().get(CASE_EVENT_HANDLER_MESSAGE_HEALTH));
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void testHealthDoesNotCallRepositoryIfNonWorkingDayWeekend() {
        // GIVEN
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
    void testHealthDoesNotCallRepositoryIfWorkingDayTimeIsOutOfWorkingHours(LocalDateTime outOfWorkingHoursDate) {
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
    void testHealthCallsRepositoryIfWorkingDayTimeIsWithinWorkingHours(LocalDateTime withinWorkingHoursDate) {
        // GIVEN
        setupMockClock(withinWorkingHoursDate);

        receivedMessagesHealthController.health();

        verify(caseEventMessageRepository).getNumberOfMessagesReceivedInLastHour(withinWorkingHoursDate.minusHours(1));
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
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 8, 29),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 18, 31),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 0, 0)

        );
    }

    private static Stream<LocalDateTime> workingHoursScenarioProvider() {
        return Stream.of(
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 9, 30),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 9, 31),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 18, 30),
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 18, 29)
        );
    }
}

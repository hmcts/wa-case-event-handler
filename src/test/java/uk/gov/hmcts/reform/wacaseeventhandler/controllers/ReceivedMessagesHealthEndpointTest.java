package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class ReceivedMessagesHealthEndpointTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private Clock clock;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private ReceivedMessagesHealthEndpoint receivedMessagesHealthEndpoint;

    @BeforeEach
    void setup() {
        setupDefaultMockClock();
    }

    @Test
    void testHealthReportsErrorIfNoMessagesReceivedInLastHour() {
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(0);
        assertEquals(DOWN, receivedMessagesHealthEndpoint.health().getStatus());
    }

    @Test
    void testHealthReportsSuccessIfMessagesReceivedInLastHour() {
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(1);
        assertEquals(UP, receivedMessagesHealthEndpoint.health().getStatus());
    }

    @Test
    void testHealthDoesNotCallRepositoryIfNonWorkingDayHoliday() {
        when(holidayService.isHoliday(any(LocalDate.class))).thenReturn(true);

        assertEquals(UP, receivedMessagesHealthEndpoint.health().getStatus());

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void testHealthDoesNotCallRepositoryIfNonWorkingDayWeekend() {
        when(holidayService.isWeekend(any(LocalDate.class))).thenReturn(true);
        assertEquals(UP, receivedMessagesHealthEndpoint.health().getStatus());
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @ParameterizedTest
    @MethodSource(value = "nonWorkingHoursScenarioProvider")
    void testHealthDoesNotCallRepositoryIfWorkingDayTimeIsOutOfWorkingHours(LocalDateTime outOfWorkingHoursDate) {
        setupMockClock(outOfWorkingHoursDate);

        receivedMessagesHealthEndpoint.health();

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(outOfWorkingHoursDate);
    }

    @ParameterizedTest
    @MethodSource(value = "workingHoursScenarioProvider")
    void testHealthCallsRepositoryIfWorkingDayTimeIsWithinWorkingHours(LocalDateTime withinWorkingHoursDate) {
        setupMockClock(withinWorkingHoursDate);

        receivedMessagesHealthEndpoint.health();

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
            LocalDateTime.of(2022, Month.SEPTEMBER, 01, 17, 31),
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

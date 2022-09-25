package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthEndpoint.CASE_EVENT_HANDLER_MESSAGE_HEALTH;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthEndpoint.MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthEndpoint.NO_MESSAGES_RECEIVED;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthEndpoint.NO_MESSAGE_CHECK;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=false"})
@Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_case_event_messages.sql"})
public class ReceivedMessagesHealthEndpointIT {

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
    void testHealthReportsDownIfNoMessagesReceivedInLastHourDuringWorkingHours() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 17,15));

        //THEN
        assertReceivedMessagesHealthStatus(DOWN, NO_MESSAGES_RECEIVED);
    }

    @Test
    void testHealthReportsUpIfMessagesReceivedInLastHourDuringWorkingHours() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 12,15));

        // THEN
        assertReceivedMessagesHealthStatus(UP, MESSAGES_RECEIVED);
    }

    @Test
    void testHealthReportsUpIfNoMessagesReceivedInLastHourDuringWeekend() throws Exception {
        // GIVEN
        LocalDateTime localDateTime = LocalDateTime.of(2022, 8, 28, 12, 15);
        setClock(localDateTime);

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(holidayService).isWeekend(localDateTime.toLocalDate());
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void testHealthReportsUpIfNoMessagesReceivedInLastHourDuringHoliday() throws Exception {
        // GIVEN
        LocalDateTime localDateTime = LocalDateTime.of(2022, 8, 29, 12,15);
        setClock(localDateTime);

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(holidayService).isHoliday(localDateTime.toLocalDate());
        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void testHealthReportsUpIfTimeOutsideOfWorkingHoursStartTime() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 9,29));

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    @Test
    void testHealthReportsUpIfTimeOutsideOfWorkingHoursEndTime() throws Exception {
        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 18,31));

        // THEN
        assertReceivedMessagesHealthStatus(UP, NO_MESSAGE_CHECK);

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
    }

    private void assertReceivedMessagesHealthStatus(Status status, String details) throws Exception {
        mockMvc.perform(get("/ccdMessagesReceived/health"))
            .andExpect(
                jsonPath("$.status")
                    .value(status.toString()))
            .andExpect(jsonPath("$.details." + CASE_EVENT_HANDLER_MESSAGE_HEALTH)
                           .value(details));
    }

    private void setClock(LocalDateTime localDateTime) {
        when(clock.instant()).thenReturn(localDateTime.toInstant(ZoneOffset.UTC));
        ZoneId zoneId = mock(ZoneId.class);
        ZoneRules zoneRules = mock(ZoneRules.class);

        when(zoneRules.getOffset(any(Instant.class))).thenReturn(ZoneOffset.UTC);
        when(zoneId.getRules()).thenReturn(zoneRules);
        when(clock.getZone()).thenReturn(zoneId);
    }
}

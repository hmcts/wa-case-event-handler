package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CASE_EVENT_HANDLER_MESSAGE_HEALTH;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.ReceivedMessagesHealthController.CHECK_DISABLED_MESSAGE;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=false",
    "environment=aat",
    "management.endpoint.health.receivedMessageCheckEnvEnabled=aat"})
@Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_case_event_messages.sql"})
public class ReceivedMessagesHealthControllerStagingEnvTest {

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
    void test_health_reports_up_api_invoked_from_staging() throws Exception {

        // GIVEN
        setClock(LocalDateTime.of(2022, 8, 26, 11,29));

        // THEN
        mockMvc.perform(get("/health").header("Host", "case-event-handler.aat.staging"))
            .andExpect(
                jsonPath("$.components.ccdMessagesReceived.status")
                    .value(UP.toString()))
            .andExpect(jsonPath("$.components.ccdMessagesReceived.details." + CASE_EVENT_HANDLER_MESSAGE_HEALTH)
                           .value(String.format(CHECK_DISABLED_MESSAGE, "aat")));

        verify(caseEventMessageRepository, never()).getNumberOfMessagesReceivedInLastHour(any());
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

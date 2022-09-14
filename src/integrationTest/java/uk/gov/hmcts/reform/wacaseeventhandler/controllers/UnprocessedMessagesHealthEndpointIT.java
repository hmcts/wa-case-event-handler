package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=false",
    "management.endpoint.health.newMessageStateThreshold=2"})
public class UnprocessedMessagesHealthEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_case_event_messages.sql"})
    @Test
    void testHealthReportsDownIfMessagesInNewStateGreaterThanThreshold() throws Exception {
        assertReceivedMessagesHealthStatus(DOWN, 3);
    }

    @Sql("classpath:sql/delete_from_case_event_messages.sql")
    @Test
    void testHealthReportsDownIfMessagesInNewStateFewerThanThreshold() throws Exception {
        assertReceivedMessagesHealthStatus(UP, 0);
    }

    private void assertReceivedMessagesHealthStatus(Status status, int numMessages) throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(
                jsonPath("$.components.ccdMessagesInNewState.status")
                    .value(status.toString())
                    )
            .andExpect(
                jsonPath("$.components.ccdMessagesInNewState.details.caseEventHandlerMessageStateHealth")
                    .value(numMessages + " messages in NEW state")
            );
    }
}

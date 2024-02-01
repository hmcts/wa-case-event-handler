package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"environment=test"})
public class CaseEventHandlerReadinessHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private CaseEventMessageRepository caseEventMessageRepository;

    @BeforeEach
    void setup() {
        reset(caseEventMessageRepository);
    }

    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_case_event_messages.sql"})
    void test_readiness_health_for_success() throws Exception {
        assertReadinessHealthStatus(UP);
    }

    @Test
    void test_readiness_health_for_failure() throws Exception {
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenThrow(
            new PSQLException(new ServerErrorMessage("An I/O error occurred while sending to the backend")));

        assertReadinessHealthStatus(OUT_OF_SERVICE);
    }

    private void assertReadinessHealthStatus(Status status) throws Exception {
        mockMvc.perform(get("/health/readiness"))
            .andExpect(
                jsonPath("$.components.CEHReadinessHealthController.status")
                    .value(status.toString()));
    }

}

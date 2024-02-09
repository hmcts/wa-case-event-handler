package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wacaseeventhandler.Application;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.reset;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"environment=test"})
public class CaseEventHandlerLivenessHealthControllerTest {

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
    void test_liveness_health_for_success() throws Exception {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        assertLivenessHealthStatus(UP);
    }

    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql", "classpath:scripts/insert_100_case_event_messages.sql"})
    void test_liveness_health_for_failure() throws Exception {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        assertLivenessHealthStatus(DOWN);
    }

    private void assertLivenessHealthStatus(Status status) throws Exception {
        mockMvc.perform(get("/health/liveness"))
            .andExpect(
                jsonPath("$.components.caseEventHandlerLivenessHealthController.status")
                    .value(status.toString()));
    }

    @Configuration
    @Import(Application.class)
    public static class TestConfiguration {

        static FakeTicker fakeTicker = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return fakeTicker::read;
        }

    }

}

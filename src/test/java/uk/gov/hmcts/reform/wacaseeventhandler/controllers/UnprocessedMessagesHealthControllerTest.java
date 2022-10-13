package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.UnprocessedMessagesHealthController.CHECK_DISABLED_MESSAGE;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.UnprocessedMessagesHealthController.RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE;

@ExtendWith(MockitoExtension.class)
class UnprocessedMessagesHealthControllerTest {

    private static final Integer THRESHOLD = 10;

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @InjectMocks
    private UnprocessedMessagesHealthController unprocessedMessagesHealthController;

    @BeforeEach
    void setup() {
        setField(unprocessedMessagesHealthController, "newMessageStateThreshold", THRESHOLD);
        setField(unprocessedMessagesHealthController, "newMessageStateCheckEnvEnabled", "validEnvironment,test");
        setField(unprocessedMessagesHealthController, "environment", "validEnvironment");
    }

    @Test
    void health_returns_up_when_new_message_count_less_than_threshold() {
        int underThresholdValue = THRESHOLD - 1;
        when(caseEventMessageRepository.getNumberOfMessagesInNewState()).thenReturn(underThresholdValue);

        Health health = unprocessedMessagesHealthController.health();

        assertEquals(UP, health.getStatus());
        assertTrue(health.getDetails()
                       .containsValue(String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, underThresholdValue)));
    }

    @Test
    void health_returns_down_when_new_message_count_greater_than_threshold() {
        int overThresholdValue = THRESHOLD + 1;
        when(caseEventMessageRepository.getNumberOfMessagesInNewState()).thenReturn(overThresholdValue);

        Health health = unprocessedMessagesHealthController.health();

        assertEquals(DOWN, health.getStatus());
        assertTrue(health.getDetails()
                       .containsValue(String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, overThresholdValue)));
    }

    @Test
    void health_returns_up_when_check_disabled_in_current_environment() {
        String environment = "invalidEnvironment";
        setField(unprocessedMessagesHealthController, "environment", environment);

        Health health = unprocessedMessagesHealthController.health();

        verify(caseEventMessageRepository, never()).getNumberOfMessagesInNewState();
        assertEquals(UP, health.getStatus());
        assertTrue(health.getDetails()
                       .containsValue(String.format(CHECK_DISABLED_MESSAGE, environment)));
    }

}

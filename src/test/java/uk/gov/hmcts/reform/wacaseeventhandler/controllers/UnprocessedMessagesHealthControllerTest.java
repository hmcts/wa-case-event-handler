package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
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
        ReflectionTestUtils.setField(unprocessedMessagesHealthController, "newMessageStateThreshold", THRESHOLD);
    }

    @Test
    void healthReturnsUpWhenNewMessageCountLessThanThreshold() {
        int underThresholdValue = THRESHOLD - 1;
        when(caseEventMessageRepository.getNumberOfMessagesInNewState()).thenReturn(underThresholdValue);

        Health health = unprocessedMessagesHealthController.health();

        assertEquals(UP, health.getStatus());
        assertTrue(health.getDetails()
                       .containsValue(String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, underThresholdValue)));
    }

    @Test
    void healthReturnsDownWhenNewMessageCountGreaterThanThreshold() {
        int overThresholdValue = THRESHOLD + 1;
        when(caseEventMessageRepository.getNumberOfMessagesInNewState()).thenReturn(overThresholdValue);

        Health health = unprocessedMessagesHealthController.health();

        assertEquals(DOWN, health.getStatus());
        assertTrue(health.getDetails()
                       .containsValue(String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, overThresholdValue)));
    }
}

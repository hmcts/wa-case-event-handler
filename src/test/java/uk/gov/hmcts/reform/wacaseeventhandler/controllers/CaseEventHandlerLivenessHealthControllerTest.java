package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageCacheService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
public class CaseEventHandlerLivenessHealthControllerTest {

    @Mock
    private CaseEventMessageCacheService caseEventMessageCacheService;

    @Mock
    ApplicationAvailability availability;

    @InjectMocks
    private CaseEventHandlerLivenessHealthController caseEventHandlerLivenessHealthController;

    @BeforeEach
    void setup() {
        setField(caseEventHandlerLivenessHealthController, "newMessageLivenessStateCheckEnvEnabled",
                 "validEnvironment,test");
        setField(caseEventHandlerLivenessHealthController, "environment", "validEnvironment");
    }

    @Test
    void test_get_state_for_success_when_no_new_messages() {
        // GIVEN
        when(caseEventMessageCacheService.getAllMessagesInNewState("validEnvironment")).thenReturn(Collections.emptyList());

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
    }

    @Test
    void test_get_state_for_success_with_messages_over_threshold_and_still_processing() {

        List<CaseEventMessageEntity> messages = new ArrayList<>();

        IntStream.range(1,105).forEach(x -> {
            CaseEventMessageEntity caseEventMessage = new CaseEventMessageEntity();
            caseEventMessage.setSequence((long) x);
            caseEventMessage.setCaseId("1234");
            caseEventMessage.setMessageId("ABCD");
            caseEventMessage.setState(MessageState.NEW);
            caseEventMessage.setReceived(LocalDateTime.now().minusMinutes(45));
            caseEventMessage.setEventTimestamp(LocalDateTime.now().minusMinutes(50));
            messages.add(caseEventMessage);
        });

        // GIVEN
        when(caseEventMessageCacheService.getAllMessagesInNewState("validEnvironment")).thenReturn(messages);

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
    }

    @Test
    void test_get_state_for_success_if_messages_check_disabled_in_current_environment() {
        // GIVEN
        setField(caseEventHandlerLivenessHealthController, "environment", "invalidEnvironment");
        setField(caseEventHandlerLivenessHealthController, "newMessageLivenessStateCheckEnvEnabled",
                 "validEnvironment,test");

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
        verifyNoInteractions(caseEventMessageCacheService);
    }

    @Test
    void test_get_state_for_failure() {

        List<CaseEventMessageEntity> messages = new ArrayList<>();

        IntStream.range(1,105).forEach(x -> {
            CaseEventMessageEntity caseEventMessage = new CaseEventMessageEntity();
            caseEventMessage.setSequence((long) x);
            caseEventMessage.setCaseId("1234");
            caseEventMessage.setMessageId("ABCD");
            caseEventMessage.setState(MessageState.NEW);
            caseEventMessage.setReceived(LocalDateTime.now().minusMinutes(90));
            caseEventMessage.setEventTimestamp(LocalDateTime.now().minusHours(2L));
            messages.add(caseEventMessage);
        });

        // GIVEN
        when(caseEventMessageCacheService.getAllMessagesInNewState("validEnvironment")).thenReturn(messages);

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(DOWN, health.getStatus());
    }
}

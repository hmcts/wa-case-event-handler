package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

@ExtendWith(MockitoExtension.class)
public class CaseEventHandlerLivenessHealthControllerTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    ApplicationAvailability availability;

    @InjectMocks
    private CaseEventHandlerLivenessHealthController caseEventHandlerLivenessHealthController;


    @Test
    void test_get_state_for_success() {
        // GIVEN
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(Collections.emptyList());

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
    }

    @Test
    void test_get_state_for_failure() {

        List<CaseEventMessageEntity> messages = new ArrayList<>();
        IntStream.range(0, 105).forEach(
            x -> messages.add(new CaseEventMessageEntity()));

        // GIVEN
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(messages);

        // WHEN
        Health health = caseEventHandlerLivenessHealthController.health();

        // THEN
        assertEquals(DOWN, health.getStatus());
    }
}

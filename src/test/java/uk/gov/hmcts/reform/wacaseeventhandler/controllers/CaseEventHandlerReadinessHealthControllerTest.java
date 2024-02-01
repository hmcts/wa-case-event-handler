package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;
import static org.springframework.boot.actuate.health.Status.UP;

@ExtendWith(MockitoExtension.class)
public class CaseEventHandlerReadinessHealthControllerTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    ApplicationAvailability availability;

    @InjectMocks
    private CaseEventHandlerReadinessHealthController caseEventHandlerReadinessHealthController;


    @Test
    void test_get_state_for_success() {
        // GIVEN
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenReturn(5);

        // WHEN
        Health health = caseEventHandlerReadinessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
    }

    @Test
    void test_get_state_for_failure() {

        // GIVEN
        when(caseEventMessageRepository.getNumberOfMessagesReceivedInLastHour(any())).thenThrow(
            new RuntimeException("An I/O error occurred while sending to the backend"));

        // WHEN
        Health health = caseEventHandlerReadinessHealthController.health();

        // THEN
        assertEquals(OUT_OF_SERVICE, health.getStatus());
    }
}

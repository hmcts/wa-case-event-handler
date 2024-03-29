package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.ApplicationAvailability;
import org.testcontainers.exception.ConnectionCreationException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageCacheService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerReadinessHealthControllerTest {

    @Mock
    private CaseEventMessageCacheService caseEventMessageCacheService;

    @Mock
    ApplicationAvailability availability;

    @InjectMocks
    private CaseEventHandlerReadinessHealthController caseEventHandlerReadinessHealthController;

    @BeforeEach
    void setup() {
        setField(caseEventHandlerReadinessHealthController, "environment", "validEnvironment");
    }

    @Test
    void test_get_state_for_success() {
        // GIVEN
        when(caseEventMessageCacheService.getAllMessagesInNewState("validEnvironment"))
            .thenReturn(Collections.emptyList());

        // WHEN
        Health health = caseEventHandlerReadinessHealthController.health();

        // THEN
        assertEquals(UP, health.getStatus());
    }

    @Test
    void test_get_state_for_failure() {

        // GIVEN
        when(caseEventMessageCacheService.getAllMessagesInNewState("validEnvironment")).thenThrow(
            new ConnectionCreationException("Unable to connect to DB",
                                            new Throwable("An I/O error occurred while sending to the backend")));

        // WHEN
        Health health = caseEventHandlerReadinessHealthController.health();

        // THEN
        assertEquals(OUT_OF_SERVICE, health.getStatus());
    }
}

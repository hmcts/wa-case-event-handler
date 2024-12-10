package uk.gov.hmcts.reform.wacaseeventhandler.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.LoggingUtilityFailure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class LoggingUtilityTest {

    @Test
    void logPrettyPrintStringArgument() {
        String input = "{\"job_details\" : {\"name\" : \"FIND_PROBLEM_MESSAGES\"}}";

        @SuppressWarnings("PMD.LawOfDemeter")
        String output = LoggingUtility.logPrettyPrint(input);

        String expectedOutput = "{job_details: {name: FIND_PROBLEM_MESSAGES}}";

        assertEquals(expectedOutput, output, "output does not match expected output");
        assertNotEquals(output, input, "output can't be equal to input");
    }

    @Test
    void logPrettyPrintStringArgumentShouldThrowException() {
        assertThrows(LoggingUtilityFailure.class, () -> LoggingUtility.logPrettyPrint("invalid input"));
    }

    @Test
    void logPrettyPrintObjectArgument() {
        CaseEventMessage input = CaseEventMessage.builder()
            .messageId("some message id")
            .caseId("someCaseId")
            .state(MessageState.NEW)
            .build();

        @SuppressWarnings("PMD.LawOfDemeter")
        String output = LoggingUtility.logPrettyPrint(input);

        String expectedOutput = "{messageId: some message id, " +
            "sequence: null, caseId: someCaseId, eventTimestamp: null, fromDlq: null, state: NEW, " +
            "messageProperties: null, messageContent: null, received: null, deliveryCount: null, " +
            "holdUntil: null, retryCount: null}";

        assertEquals(expectedOutput, output, "output does not match expected output");
    }

    @Test
    void logPrettyPrintObjectArgumentArgumentShouldThrowException() {
        assertThrows(LoggingUtilityFailure.class, () -> LoggingUtility.logPrettyPrint(new Object()));
    }

}

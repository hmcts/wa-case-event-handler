package uk.gov.hmcts.reform.wacaseeventhandler.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;

@ExtendWith({MockitoExtension.class})
class EventMessageReceiverServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Mock
    private JsonProcessingException jsonProcessingException;

    @InjectMocks
    private EventMessageReceiverService eventMessageReceiverService;

    private static final String USER_ID = "123";
    private static final String MESSAGE_ID = "messageId";
    private static final String MESSAGE = "message";

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() throws JsonProcessingException {
        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageReceiverService.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId("JUR")
                        .caseTypeId("CASEID")
                        .build());
    }

    @Test
    void test_handle_ccd_case_event_asb_message_feature_flag_disabled() {
        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(FALSE);

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, "message");

        assertLogMessageEquals(String.format("Received CCD Case Events ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(
                String.format("Feature flag '%s' evaluated to false. Message not inserted into DB",
                        DLQ_DB_INSERT.getKey()), 1);
    }

    @Test
    void test_handle_ccd_case_event_asb_message_feature_flag_enabled() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation.builder()
                        .userId(USER_ID)
                        .jurisdictionId("JUR")
                        .caseTypeId("CASEID")
                        .build());
        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(TRUE);

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Received CCD Case Events ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageContains("Case Event Dead Letter Queue details:", 1);
        assertLogMessageEquals(String.format("Message with id '%s' successfully stored into the DB", MESSAGE_ID), 2);
    }

    @Test
    void test_handle_ccd_case_event_asb_message_event_information_parsing_failed()
            throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Received CCD Case Events ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);
    }

    @Test
    void test_handle_asb_message() {
        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Received ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageContains("Case Event details:", 1);
    }

    @Test
    void test_handle_asb_message_event_information_parsing_failed() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Received ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);
    }

    @Test
    void test_handle_dlq_message() {
        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(
                String.format("Received Case Event Dead Letter Queue message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageContains("Case Event Dead Letter Queue details:", 1);
    }

    @Test
    void test_handle_dlq_message_event_information_parsing_failed() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(
                String.format("Received Case Event Dead Letter Queue message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);
    }

    private void assertLogMessageEquals(String expectedMessage, int messageNumber)  {
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(expectedMessage, logsList.get(messageNumber).getFormattedMessage());
    }

    private void assertLogMessageContains(String expectedMessage, int messageNumber)  {
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(messageNumber).getFormattedMessage().startsWith(expectedMessage));
    }
}
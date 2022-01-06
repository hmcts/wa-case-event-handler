package uk.gov.hmcts.reform.wacaseeventhandler.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageDuplicateMessageIdException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService.MESSAGE_PROPERTIES;

@ExtendWith({MockitoExtension.class})
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
class EventMessageReceiverServiceTest {
    private static final String USER_ID = "123";
    private static final String MESSAGE_ID = "messageId";
    private static final String MESSAGE = messageAsString();
    private static final String JURISDICTION = "JUR";
    private static final String CASE_TYPE_ID = "CASETYPEID";
    private static final String CASE_ID = "12345";
    private static final LocalDateTime EVENT_TIME_STAMP = LocalDateTime.now();
    private static final LocalDateTime RECEIVED = LocalDateTime.now();

    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Mock
    private JsonProcessingException jsonProcessingException;

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Spy
    private CaseEventMessageMapper caseEventMessageMapper = new CaseEventMessageMapper();

    @Captor
    private ArgumentCaptor<CaseEventMessageEntity> caseEventMessageEntityCaptor;

    @InjectMocks
    private EventMessageReceiverService eventMessageReceiverService;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageReceiverService.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        eventMessageReceiverService = new EventMessageReceiverService(objectMapper,
                                                                      caseEventMessageRepository,
                                                                      caseEventMessageMapper,
                                                                      featureFlagProvider);
    }

    @Test
    void should_handle_message_event_information_when_parsing_failed() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
    }

    @Test
    void should_handle_message_when_valid_message_received() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessage result = eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertFalse(caseEventMessageEntityCaptor.getValue().getFromDlq());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertEquals(CASE_ID, result.getCaseId());
        assertNotNull(result.getEventTimestamp());
        assertEquals(false, result.getFromDlq());
        assertEquals(MessageState.NEW, result.getState());
        assertEquals(getMessagesPropertyAsJson().findPath(MESSAGE_PROPERTIES), result.getMessageProperties());
        assertEquals(MESSAGE, result.getMessageContent());
        assertNotNull(result.getReceived());
        assertEquals(0, result.getDeliveryCount());
        assertEquals(0, result.getRetryCount());
    }

    @Test
    void should_handle_message_when_invalid_message_received() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(EventInformation.builder()
                            .userId(USER_ID)
                            .jurisdictionId(JURISDICTION)
                            .caseTypeId(CASE_TYPE_ID)
                            .build());

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_invalid_message_deserialization() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenThrow(jsonProcessingException);
        CaseEventMessage result = eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MESSAGE_ID, result.getMessageId());
        assertNull(result.getCaseId());
        assertNull(result.getEventTimestamp());
        assertEquals(false, result.getFromDlq());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
        assertNull(result.getMessageProperties());
        assertEquals(MESSAGE, result.getMessageContent());
        assertNotNull(result.getReceived());
        assertEquals(0, result.getDeliveryCount());
        assertEquals(0, result.getRetryCount());
    }

    @Test
    void should_handle_message_when_message_parsing_fails() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(
            String.format("Could not parse the message with id '%s'", MESSAGE_ID), 1);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_message_when_data_integrity_violation_occurs() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(EventInformation.builder()
                            .userId(USER_ID)
                            .jurisdictionId(JURISDICTION)
                            .caseTypeId(CASE_TYPE_ID)
                            .build());

        when(caseEventMessageRepository.save(any(CaseEventMessageEntity.class)))
            .thenThrow(new DataIntegrityViolationException("Exception message"));

        final CaseEventMessageDuplicateMessageIdException caseEventMessageDuplicateMessageIdException =
            assertThrows(CaseEventMessageDuplicateMessageIdException.class,
                () -> eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE));

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());

        assertEquals(String.format("Trying to save a message with a duplicate messageId: %s", MESSAGE_ID),
                     caseEventMessageDuplicateMessageIdException.getMessage());
    }

    @Test
    void should_handle_dlq_message_when_feature_flag_disabled() throws JsonProcessingException {
        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(FALSE);

        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE))
                .thenReturn(jsonNode);

        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        verifyNoInteractions(caseEventMessageRepository);
        assertLogMessageEquals(String.format("Received Case Event Dead Letter Queue message with id '%s'", MESSAGE_ID),
                0);
        assertLogMessageEquals(
                String.format("Feature flag '%s' evaluated to false. Message not inserted into database",
                        DLQ_DB_INSERT.getKey()), 2);
    }

    @Test
    void should_handle_dlq_message_when_feature_flag_enabled_and_valid_message_received()
            throws JsonProcessingException {
        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE))
                .thenReturn(jsonNode);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(TRUE);

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId("CASEID")
                        .caseId("CASEID")
                        .eventTimeStamp(LocalDateTime.now())
                        .build());


        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
        assertTrue(caseEventMessageEntityCaptor.getValue().getFromDlq());
    }

    @Test
    void should_handle_dlq_case_event_asb_message_when_feature_flag_enabled_and_invalid_message_received()
            throws JsonProcessingException {
        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE)).thenReturn(jsonNode);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(TRUE);

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId("CASEID")
                        .build());

        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_dlq_message_when_error_parsing_user_id()
            throws JsonProcessingException {
        when(objectMapper.readTree(MESSAGE)).thenThrow(jsonProcessingException);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, null))
                .thenCallRealMethod();

        final NullPointerException nullPointerException = assertThrows(NullPointerException.class, () ->
                eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE));

        assertEquals("userId is null", nullPointerException.getMessage());
        verifyNoInteractions(caseEventMessageRepository);
    }

    @Test
    void should_handle_dlq_message_when_missing_user_id()
            throws JsonProcessingException {

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, null))
                .thenCallRealMethod();

        String noUserIdJson = "{"
                + "\"IdOfUser\": \"" + USER_ID + "\""
                + "}";

        final JsonNode jsonNode = new ObjectMapper().readTree(noUserIdJson);

        when(objectMapper.readTree(MESSAGE)).thenReturn(jsonNode);

        final NullPointerException nullPointerException = assertThrows(NullPointerException.class, () ->
                eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE));

        assertEquals("userId is null", nullPointerException.getMessage());
        verifyNoInteractions(caseEventMessageRepository);
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_feature_flag_disabled() throws JsonProcessingException {
        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(FALSE);

        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE))
                .thenReturn(jsonNode);

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        verifyNoInteractions(caseEventMessageRepository);
        assertLogMessageEquals(String.format("Received CCD Case Events ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(
                String.format("Feature flag '%s' evaluated to false. Message not inserted into database",
                        DLQ_DB_INSERT.getKey()), 2);
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_feature_flag_enabled_and_valid_message_received()
            throws JsonProcessingException {
        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE))
                .thenReturn(jsonNode);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(TRUE);

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId("CASEID")
                        .caseId("CASEID")
                        .eventTimeStamp(LocalDateTime.now())
                        .build());

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_feature_flag_enabled_and_invalid_message_received()
            throws JsonProcessingException {
        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE)).thenReturn(jsonNode);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(TRUE);

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId("CASEID")
                        .build());

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_error_parsing_user_id()
            throws JsonProcessingException {
        when(objectMapper.readTree(MESSAGE)).thenThrow(jsonProcessingException);

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, null))
                .thenCallRealMethod();

        final NullPointerException nullPointerException = assertThrows(NullPointerException.class, () ->
                eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE));

        assertEquals("userId is null", nullPointerException.getMessage());
        verifyNoInteractions(caseEventMessageRepository);
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_missing_user_id()
            throws JsonProcessingException {

        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, null))
                .thenCallRealMethod();

        String noUserIdJson = "{"
                + "\"IdOfUser\": \"" + USER_ID + "\""
                + "}";

        final JsonNode jsonNode = new ObjectMapper().readTree(noUserIdJson);

        when(objectMapper.readTree(MESSAGE)).thenReturn(jsonNode);

        final NullPointerException nullPointerException = assertThrows(NullPointerException.class, () ->
                eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE));

        assertEquals("userId is null", nullPointerException.getMessage());
        verifyNoInteractions(caseEventMessageRepository);
    }

    @Test
    void should_get_message_by_message_id_when_message_found() throws JsonProcessingException {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();

        entity.setMessageId(MESSAGE_ID);
        entity.setSequence(4L);
        entity.setCaseId(CASE_ID);
        entity.setEventTimestamp(EVENT_TIME_STAMP);
        entity.setFromDlq(true);
        entity.setState(MessageState.NEW);
        entity.setMessageProperties(new ObjectMapper().readValue("{\"Property1\":\"Test\"}", JsonNode.class));
        String messageContent = "{\"CaseId\":\"12345\",\"MessageProperties\":{\"Property1\":\"Test\"}}";
        entity.setMessageContent(messageContent);
        entity.setReceived(RECEIVED);
        entity.setDeliveryCount(1);
        entity.setHoldUntil(RECEIVED.plusDays(2));
        entity.setRetryCount(2);

        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        final CaseEventMessage message = eventMessageReceiverService.getMessage(MESSAGE_ID);

        assertNotNull(message);
        assertEquals(MESSAGE_ID, message.getMessageId());
        assertEquals(CASE_ID, message.getCaseId());
        assertEquals(4L, message.getSequence());
        assertNotNull(message.getEventTimestamp());
        assertEquals(true, message.getFromDlq());
        assertEquals(MessageState.NEW, message.getState());
        assertNotNull(message.getMessageProperties());
        assertEquals(messageContent, message.getMessageContent());
        assertEquals(RECEIVED, message.getReceived());
        assertEquals(1, message.getDeliveryCount());
        assertEquals(RECEIVED.plusDays(2), message.getHoldUntil());
        assertEquals(2, message.getRetryCount());
    }

    @Test
    void should_return_message_not_found_exception_when_no_message_found() {
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(Collections.emptyList());
        CaseEventMessageNotFoundException caseEventMessageNotFoundException =
            assertThrows(CaseEventMessageNotFoundException.class,
                () -> eventMessageReceiverService.getMessage(MESSAGE_ID));
        assertEquals(String.format("Could not find a message with message id: %s", MESSAGE_ID),
                     caseEventMessageNotFoundException.getMessage());
    }

    private void assertLogMessageEquals(String expectedMessage, int messageNumber)  {
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(expectedMessage, logsList.get(messageNumber).getFormattedMessage());
    }

    private static String messageAsString() {
        return "{\n"
            + "  \"EventInstanceId\" : \"some event instance Id\",\n"
            + "  \"EventTimeStamp\" : \"2020-12-07T17:39:22.232622\",\n"
            + "  \"CaseId\" : \"12345\",\n"
            + "  \"JurisdictionId\" : \"ia\",\n"
            + "  \"CaseTypeId\" : \"asylum\",\n"
            + "  \"EventId\" : \"some event Id\",\n"
            + "  \"NewStateId\" : \"some new state Id\",\n"
            + "  \"UserId\" : \"some user Id\",\n"
            + "  \"MessageProperties\" : {\n"
            + "      \"property1\" : \"test1\"\n"
            + "  }\n"
            + "}";
    }

    private void mockMessageProperties() throws JsonProcessingException {
        when(objectMapper.readTree(any(String.class))).thenReturn(getMessagesPropertyAsJson());
    }

    private JsonNode getMessagesPropertyAsJson() throws JsonProcessingException {
        return new ObjectMapper().readTree("{\"" + MESSAGE_PROPERTIES + "\":{\"property1\":\"test1\"}}");
    }

    private EventInformation getEventInformation() {
        return EventInformation.builder()
            .userId(USER_ID)
            .jurisdictionId(JURISDICTION)
            .caseTypeId(CASE_TYPE_ID)
            .caseId(CASE_ID)
            .eventTimeStamp(LocalDateTime.now())
            .build();
    }
}

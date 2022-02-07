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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationMetadata;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageDuplicateMessageIdException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    void should_handle_message_when_valid_message_received() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.save(any())).thenReturn(entity);
        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertFalse(caseEventMessageEntityCaptor.getValue().getFromDlq());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_message_event_information_when_parsing_failed() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
    }

    @Test
    void handle_message_event_message_properties_parsing_failed() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockParsingExceptionWhenRetrievingMessageProperties();

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 2);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
    }

    @Test
    void should_handle_message_when_invalid_message_received() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(EventInformation.builder()
                            .userId(USER_ID)
                            .jurisdictionId(JURISDICTION)
                            .caseTypeId(CASE_TYPE_ID)
                            .build());
        mockMessageProperties();

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_upsert_valid_dlq_message() throws JsonProcessingException {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessage result = eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, true);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertEquals(CASE_ID, result.getCaseId());
        assertEquals(true, result.getFromDlq());
        assertEquals(entity.getSequence(), caseEventMessageEntityCaptor.getValue().getSequence());
    }

    @Test
    void should_upsert_invalid_dlq_message_with_missing_EventTimestamp() throws JsonProcessingException {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformationWithMissingEventTimeStamp());
        mockMessageProperties();

        CaseEventMessage result = eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, true);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertEquals(CASE_ID, result.getCaseId());
        assertEquals(true, result.getFromDlq());
        assertNull(result.getEventTimestamp());
        assertEquals(entity.getSequence(), caseEventMessageEntityCaptor.getValue().getSequence());
    }

    @Test
    void should_upsert_invalid_dlq_message_with_missing_CaseId() throws JsonProcessingException {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformationWithMissingCaseId());
        mockMessageProperties();

        CaseEventMessage result = eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, true);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertNotNull(result.getEventTimestamp());
        assertEquals(true, result.getFromDlq());
        assertNull(result.getCaseId());
        assertEquals(entity.getSequence(), caseEventMessageEntityCaptor.getValue().getSequence());
    }

    @Test
    void should_upsert_invalid_dlq_message_with_missing_messageId() throws JsonProcessingException {
        when(caseEventMessageRepository.findByMessageId(null)).thenReturn(List.of());

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.save(any())).thenReturn(entity);
        eventMessageReceiverService.upsertMessage(null, MESSAGE, true);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_upsert_invalid_dlq_message_with_missing_fromDlq() throws JsonProcessingException {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessage result = eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, null);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
        assertEquals(MESSAGE_ID, result.getMessageId());
        assertEquals(CASE_ID, result.getCaseId());
        assertNotNull(result.getEventTimestamp());
        assertNull(result.getFromDlq());
        assertEquals(entity.getSequence(), caseEventMessageEntityCaptor.getValue().getSequence());
    }

    @Test
    void should_upsert_invalid_dlq_message() throws JsonProcessingException {

        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));
        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenThrow(jsonProcessingException);

        CaseEventMessage result = eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, false);

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
        assertEquals(entity.getSequence(), result.getSequence());
    }

    @Test
    void should_upsert_valid_dlq_message_when_no_message_id_present() throws JsonProcessingException {
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.save(any())).thenReturn(entity);
        eventMessageReceiverService.upsertMessage(MESSAGE_ID, MESSAGE, true);

        verify(caseEventMessageRepository, times(2)).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_invalid_message_deserialization() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenThrow(jsonProcessingException);
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        when(caseEventMessageRepository.save(any())).thenReturn(entity);
        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        verify(caseEventMessageMapper).mapToCaseEventMessage(any(CaseEventMessageEntity.class));

        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void should_handle_message_when_message_parsing_fails() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
            .thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(
            String.format("Could not parse the message with id '%s'", MESSAGE_ID), 1);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
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

        doThrow(new DataIntegrityViolationException("Exception message"))
                .when(caseEventMessageRepository).save(any(CaseEventMessageEntity.class));
        mockMessageProperties();

        final CaseEventMessageDuplicateMessageIdException caseEventMessageDuplicateMessageIdException =
            assertThrows(CaseEventMessageDuplicateMessageIdException.class,
                () -> eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE));

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
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
            .thenReturn(EventInformation.builder()
                            .userId(USER_ID)
                            .jurisdictionId(JURISDICTION)
                            .caseTypeId(CASE_TYPE_ID)
                            .caseId("CASEID")
                            .eventTimeStamp(LocalDateTime.now())
                            .build());
        mockMessageProperties();

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
        mockMessageProperties();
        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
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

        mockMessageProperties();
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

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
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

        mockMessageProperties();
        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation
                        .builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId("CASEID")
                        .build());

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).findByMessageId(MESSAGE_ID);
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

    @Test
    void should_delete_message_by_message_id_when_message_found() {
        CaseEventMessageEntity entity = mock(CaseEventMessageEntity.class);
        given(entity.getSequence()).willReturn(5L);
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(entity));

        eventMessageReceiverService.deleteMessage(MESSAGE_ID);

        verify(caseEventMessageRepository).deleteById(5L);
    }

    @Test
    void should_not_delete_message_by_message_id_when_message_not_found() {
        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());

        CaseEventMessageNotFoundException caseEventMessageNotFoundException =
            assertThrows(CaseEventMessageNotFoundException.class,
                () -> eventMessageReceiverService.deleteMessage(MESSAGE_ID));
        assertEquals(String.format("Could not find a message with message id: %s", MESSAGE_ID),
                     caseEventMessageNotFoundException.getMessage());
    }

    @Test
    void should_update_delivery_count_when_saving_message_with_message_id_already_in_db()
            throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(getEventInformation());
        mockMessageProperties();

        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        entity.setDeliveryCount(0);
        when(caseEventMessageRepository.findByMessageId(any())).thenReturn(List.of(entity));
        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());

        assertEquals(1, caseEventMessageEntityCaptor.getValue().getDeliveryCount());
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
        Map<String, String> messageProperties = Map.of(
            "messageProperty1", "value1",
            "messageProperty2", "value2"
        );
        when(objectMapper.readValue(MESSAGE, EventInformationRequest.class))
            .thenReturn(new EventInformationRequest(null,
                                                    new EventInformationMetadata(messageProperties, null)));
        when(objectMapper.writeValueAsString(messageProperties)).thenReturn("jsonMessageProperties");
        when(objectMapper.readTree("jsonMessageProperties")).thenReturn(getMessagesPropertyAsJson());
    }

    private void mockParsingExceptionWhenRetrievingMessageProperties() throws JsonProcessingException {
        Map<String, String> messageProperties = Map.of(
            "messageProperty1", "value1",
            "messageProperty2", "value2"
        );
        when(objectMapper.readValue(MESSAGE, EventInformationRequest.class))
            .thenReturn(new EventInformationRequest(null,
                                                    new EventInformationMetadata(messageProperties, null)));
        when(objectMapper.writeValueAsString(messageProperties)).thenThrow(jsonProcessingException);
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

    private EventInformation getEventInformationWithMissingEventTimeStamp() {
        return EventInformation.builder()
            .userId(USER_ID)
            .jurisdictionId(JURISDICTION)
            .caseTypeId(CASE_TYPE_ID)
            .caseId(CASE_ID)
            .eventTimeStamp(null)
            .build();
    }

    private EventInformation getEventInformationWithMissingCaseId() {
        return EventInformation.builder()
            .userId(USER_ID)
            .jurisdictionId(JURISDICTION)
            .caseTypeId(CASE_TYPE_ID)
            .caseId(null)
            .eventTimeStamp(LocalDateTime.now())
            .build();
    }
}

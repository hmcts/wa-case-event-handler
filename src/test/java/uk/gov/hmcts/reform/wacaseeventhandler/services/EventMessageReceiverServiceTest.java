package uk.gov.hmcts.reform.wacaseeventhandler.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private CaseEventMessageMapper caseEventMessageMapper;

    @Captor
    private ArgumentCaptor<CaseEventMessageEntity> caseEventMessageEntityCaptor;

    @InjectMocks
    private EventMessageReceiverService eventMessageReceiverService;

    private static final String USER_ID = "123";
    private static final String MESSAGE_ID = "messageId";
    private static final String MESSAGE = "message";
    private static final String JURISDICTION = "JUR";
    private static final String CASE_TYPE_ID = "CASETYPEID";
    private static final String CASE_ID = "CASEID";

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageReceiverService.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);
    }

    @Test
    void test_handle_ccd_case_event_asb_message_feature_flag_disabled() throws JsonProcessingException {
        when(featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, USER_ID)).thenReturn(FALSE);

        String userIdJson = "{"
                + "\"UserId\": \"" + USER_ID + "\"}";

        final JsonNode jsonNode = new ObjectMapper().readTree(userIdJson);
        when(objectMapper.readTree(MESSAGE))
                .thenReturn(jsonNode);

        eventMessageReceiverService.handleCcdCaseEventAsbMessage(MESSAGE_ID, "message");

        verifyNoInteractions(caseEventMessageRepository);
        assertLogMessageEquals(String.format("Received CCD Case Events ASB message with id '%s'", MESSAGE_ID), 0);
        assertLogMessageEquals(
                String.format("Feature flag '%s' evaluated to false. Message not inserted into DB",
                        DLQ_DB_INSERT.getKey()), 2);
    }

    @Test
    void test_handle_ccd_case_event_asb_message_feature_flag_enabled_valid_message() throws JsonProcessingException {
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
    void test_handle_ccd_case_event_asb_message_feature_flag_enabled_invalid_message() throws JsonProcessingException {
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
    void test_handle_ccd_case_event_asb_message_error_parsing_user_id()
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
    void test_handle_ccd_case_event_asb_message_missing_user_id()
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
    void test_handle_message_event_information_parsing_failed() throws JsonProcessingException {
        when(objectMapper.readValue(MESSAGE, EventInformation.class)).thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(String.format("Could not parse the message with id '%s'",  MESSAGE_ID), 1);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
    }

    @Test
    void test_handle_message_valid_message() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation.builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId(CASE_TYPE_ID)
                        .caseId(CASE_ID)
                        .eventTimeStamp(LocalDateTime.now())
                        .build());
        when(objectMapper.readTree(MESSAGE)).thenReturn(NullNode.getInstance());

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void test_handle_message_invalid_message() throws JsonProcessingException {

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
    void test_handle_message_message_parsing_failure() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenThrow(jsonProcessingException);

        eventMessageReceiverService.handleAsbMessage(MESSAGE_ID, MESSAGE);

        assertLogMessageEquals(
                String.format("Could not parse the message with id '%s'", MESSAGE_ID), 1);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.UNPROCESSABLE, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void test_handle_message_data_integrity_violation() throws JsonProcessingException {

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
    void test_handle_dql_message_valid_message() throws JsonProcessingException {

        when(objectMapper.readValue(MESSAGE, EventInformation.class))
                .thenReturn(EventInformation.builder()
                        .userId(USER_ID)
                        .jurisdictionId(JURISDICTION)
                        .caseTypeId(CASE_TYPE_ID)
                        .caseId("CASEID")
                        .eventTimeStamp(LocalDateTime.now())
                        .build());
        when(objectMapper.readTree(MESSAGE)).thenReturn(NullNode.getInstance());

        eventMessageReceiverService.handleDlqMessage(MESSAGE_ID, MESSAGE);

        verify(caseEventMessageRepository).save(caseEventMessageEntityCaptor.capture());
        assertEquals(MessageState.NEW, caseEventMessageEntityCaptor.getValue().getState());
    }

    @Test
    void test_get_message_by_message_id_message_found() {
        final String caseId = "caseId";
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setCaseId(caseId);
        caseEventMessageEntity.setState(MessageState.READY);

        CaseEventMessage caseEventMessage = new CaseEventMessage();
        caseEventMessage.setCaseId(caseId);
        caseEventMessage.setState(MessageState.READY);

        when(caseEventMessageRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(caseEventMessageEntity));
        when(caseEventMessageMapper.mapToCaseEventMessage(caseEventMessageEntity)).thenReturn(caseEventMessage);

        final CaseEventMessage message = eventMessageReceiverService.getMessage(MESSAGE_ID);

        assertEquals(caseId, message.getCaseId());
        assertEquals(MessageState.READY, message.getState());
    }

    @Test
    void test_get_message_by_message_id_no_message_found() {
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
}
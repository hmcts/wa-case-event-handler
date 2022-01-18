package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.InvalidRequestParametersException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageCustomCriteriaRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService.FOUND_MESSAGES;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService.NO_MATCHING_RECORDS_FOR_THE_QUERY;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService.NO_QUERY_PARAMETERS_SPECIFIED;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService.NO_RECORDS_IN_THE_DATABASE;

@ExtendWith({MockitoExtension.class})
class EventMessageQueryServiceTest {

    List<MessageState> STATES = List.of(MessageState.NEW);
    String CASE_ID = "1111222233334444";
    LocalDateTime EVENT_TIMESTAMP = LocalDateTime.parse("2020-03-27T12:56:10.403975");
    Boolean NOT_FROM_DLQ = Boolean.FALSE;

    @Mock
    private CaseEventMessageCustomCriteriaRepository repository;
    @Mock
    private CaseEventMessageMapper mapper;

    @InjectMocks
    EventMessageQueryService underTest;

    @Test
    public void shouldGetMessages() {
        given(repository.countAll()).willReturn(9L);

        CaseEventMessageEntity entity1 = mock(CaseEventMessageEntity.class);
        CaseEventMessage message1 = mock(CaseEventMessage.class);
        given(mapper.mapToCaseEventMessage(entity1)).willReturn(message1);

        CaseEventMessageEntity entity2 = mock(CaseEventMessageEntity.class);
        CaseEventMessage message2 = mock(CaseEventMessage.class);
        given(mapper.mapToCaseEventMessage(entity2)).willReturn(message2);

        List<CaseEventMessageEntity> queryMessages = List.of(entity1, entity2);
        given(repository.getMessages(List.of(), CASE_ID, EVENT_TIMESTAMP, NOT_FROM_DLQ)).willReturn(queryMessages);

        EventMessageQueryResponse result = underTest.getMessages("", CASE_ID, EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ.toString());

        assertEquals(result.getMessage(), String.format(FOUND_MESSAGES, 2));
        assertEquals(result.getNumberOfMessagesFound(), 2);
        assertEquals(result.getTotalNumberOfMessages(), 9);
        assertEquals(result.getCaseEventMessages().size(), 2);
        assertTrue(result.getCaseEventMessages().contains(message1));
        assertTrue(result.getCaseEventMessages().contains(message2));
    }

    @Test
    public void shouldGetMessagesWhenNoRecordsMatchingTheQuery() {
        given(repository.countAll()).willReturn(9L);

        List<CaseEventMessageEntity> queryMessages = List.of();
        given(repository.getMessages(STATES, CASE_ID, EVENT_TIMESTAMP, NOT_FROM_DLQ)).willReturn(queryMessages);

        EventMessageQueryResponse result = underTest.getMessages("NEW", CASE_ID, EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ.toString());

        assertEquals(result.getMessage(), NO_MATCHING_RECORDS_FOR_THE_QUERY);
        assertEquals(result.getNumberOfMessagesFound(), 0);
        assertEquals(result.getTotalNumberOfMessages(), 9);
        assertEquals(result.getCaseEventMessages().size(), 0);
    }

    @Test
    public void shouldGetMessagesWhenNoMessagesInDb() {
        given(repository.countAll()).willReturn(0L);

        EventMessageQueryResponse result = underTest.getMessages("", CASE_ID, null, "false");

        assertEquals(result.getMessage(), NO_RECORDS_IN_THE_DATABASE);
        assertEquals(result.getNumberOfMessagesFound(), 0);
        assertEquals(result.getTotalNumberOfMessages(), 0);
        assertEquals(result.getCaseEventMessages().size(), 0);
    }

    @Test
    public void shouldGetMessagesWhenNoParametersProvided() {
        given(repository.countAll()).willReturn(9L);

        EventMessageQueryResponse result = underTest.getMessages(null, null, null, null);

        assertEquals(result.getMessage(), NO_QUERY_PARAMETERS_SPECIFIED);
        assertEquals(result.getNumberOfMessagesFound(), 0);
        assertEquals(result.getTotalNumberOfMessages(), 9);
        assertEquals(result.getCaseEventMessages().size(), 0);
    }

    @Test
    public void shouldGetMessagesWhenEmptyParametersProvided() {
        given(repository.countAll()).willReturn(9L);

        EventMessageQueryResponse result = underTest.getMessages("", "", "", "");

        assertEquals(result.getMessage(), NO_QUERY_PARAMETERS_SPECIFIED);
        assertEquals(result.getNumberOfMessagesFound(), 0);
        assertEquals(result.getTotalNumberOfMessages(), 9);
        assertEquals(result.getCaseEventMessages().size(), 0);
    }

    @Test
    public void shouldThrowInvalidRequestParametersExceptionWhenStatesParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("Invalid", CASE_ID, EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ.toString()))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid states format: 'Invalid'");
    }

    @Test
    public void shouldThrowInvalidRequestParametersExceptionWhenCaseIdParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("NEW", "Invalid", EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ.toString()))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid case_id format: 'Invalid'");
    }

    @Test
    public void shouldThrowInvalidRequestParametersExceptionWhenEventTimestampParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("NEW", CASE_ID, "Invalid", NOT_FROM_DLQ.toString()))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid event_timestamp format: 'Invalid'");
    }

    @Test
    public void shouldThrowInvalidRequestParametersExceptionWhenFromDlqParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("NEW", CASE_ID, EVENT_TIMESTAMP.toString(), "Invalid"))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid from_dlq format: 'Invalid'");

    }

}

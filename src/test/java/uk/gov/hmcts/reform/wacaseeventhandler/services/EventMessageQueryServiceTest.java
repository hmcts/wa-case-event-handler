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

    private static final List<MessageState> STATES = List.of(MessageState.NEW);
    private static final String CASE_ID = "1111222233334444";
    private static final LocalDateTime EVENT_TIMESTAMP = LocalDateTime.parse("2020-03-27T12:56:10.403975");
    private static final Boolean NOT_FROM_DLQ = Boolean.FALSE;

    @Mock
    private CaseEventMessageCustomCriteriaRepository repository;
    @Mock
    private CaseEventMessageMapper mapper;

    @InjectMocks
    EventMessageQueryService underTest;

    @Test
    void shouldGetMessages() {
        given(repository.countAll()).willReturn(9L);

        CaseEventMessageEntity entity1 = mock(CaseEventMessageEntity.class);
        CaseEventMessage message1 = mock(CaseEventMessage.class);
        given(mapper.mapToCaseEventMessage(entity1)).willReturn(message1);

        CaseEventMessageEntity entity2 = mock(CaseEventMessageEntity.class);
        CaseEventMessage message2 = mock(CaseEventMessage.class);
        given(mapper.mapToCaseEventMessage(entity2)).willReturn(message2);

        List<CaseEventMessageEntity> queryMessages = List.of(entity1, entity2);
        given(repository.getMessages(List.of(), CASE_ID, EVENT_TIMESTAMP, NOT_FROM_DLQ)).willReturn(queryMessages);

        EventMessageQueryResponse result = underTest.getMessages("", CASE_ID, EVENT_TIMESTAMP.toString(),
                                                                 NOT_FROM_DLQ);

        assertEquals(String.format(FOUND_MESSAGES, 2), result.getMessage());
        assertEquals(2, result.getNumberOfMessagesFound());
        assertEquals(9, result.getTotalNumberOfMessages());
        assertEquals(2, result.getCaseEventMessages().size());
        assertTrue(result.getCaseEventMessages().contains(message1));
        assertTrue(result.getCaseEventMessages().contains(message2));
    }

    @Test
    void shouldGetMessagesWhenNoRecordsMatchingTheQuery() {
        given(repository.countAll()).willReturn(9L);

        List<CaseEventMessageEntity> queryMessages = List.of();
        given(repository.getMessages(STATES, CASE_ID, EVENT_TIMESTAMP, NOT_FROM_DLQ)).willReturn(queryMessages);

        EventMessageQueryResponse result = underTest.getMessages("NEW", CASE_ID, EVENT_TIMESTAMP.toString(),
                                                                 NOT_FROM_DLQ);

        assertEquals(NO_MATCHING_RECORDS_FOR_THE_QUERY, result.getMessage());
        assertEquals(0, result.getNumberOfMessagesFound());
        assertEquals(9, result.getTotalNumberOfMessages());
        assertEquals(0, result.getCaseEventMessages().size());
    }

    @Test
    void shouldGetMessagesWhenNoMessagesInDb() {
        given(repository.countAll()).willReturn(0L);

        EventMessageQueryResponse result = underTest.getMessages("", CASE_ID, null, null);

        assertEquals(NO_RECORDS_IN_THE_DATABASE, result.getMessage());
        assertEquals(0, result.getNumberOfMessagesFound());
        assertEquals(0, result.getTotalNumberOfMessages());
        assertEquals(0, result.getCaseEventMessages().size());
    }

    @Test
    void shouldGetMessagesWhenNoParametersProvided() {
        given(repository.countAll()).willReturn(9L);

        EventMessageQueryResponse result = underTest.getMessages(null, null, null, null);

        assertEquals(NO_QUERY_PARAMETERS_SPECIFIED, result.getMessage());
        assertEquals(0, result.getNumberOfMessagesFound());
        assertEquals(9, result.getTotalNumberOfMessages());
        assertEquals(0, result.getCaseEventMessages().size());
    }

    @Test
    void shouldGetMessagesWhenEmptyParametersProvided() {
        given(repository.countAll()).willReturn(9L);

        EventMessageQueryResponse result = underTest.getMessages("", "", "", null);

        assertEquals(NO_QUERY_PARAMETERS_SPECIFIED, result.getMessage());
        assertEquals(0, result.getNumberOfMessagesFound());
        assertEquals(9, result.getTotalNumberOfMessages());
        assertEquals(0, result.getCaseEventMessages().size());
    }

    @Test
    void shouldThrowInvalidRequestParametersExceptionWhenStatesParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("Invalid", CASE_ID, EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid states format: 'Invalid'");
    }

    @Test
    void shouldThrowInvalidRequestParametersExceptionWhenCaseIdParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("NEW", "Invalid", EVENT_TIMESTAMP.toString(), NOT_FROM_DLQ))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid case_id format: 'Invalid'");
    }

    @Test
    void shouldThrowInvalidRequestParametersExceptionWhenEventTimestampParameterInvalid() {
        given(repository.countAll()).willReturn(2L);

        assertThatThrownBy(
            () -> underTest.getMessages("NEW", CASE_ID, "Invalid", NOT_FROM_DLQ))
            .isInstanceOf(InvalidRequestParametersException.class)
            .hasMessage("Invalid event_timestamp format: 'Invalid'");
    }
}

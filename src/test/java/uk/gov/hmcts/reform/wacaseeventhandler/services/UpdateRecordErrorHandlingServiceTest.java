package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.hibernate.annotations.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateRecordErrorHandlingServiceTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private CaseEventMessageEntity messageEntity;

    @InjectMocks
    private UpdateRecordErrorHandlingService updateRecordErrorHandlingService;

    @BeforeEach
    public void setUp() {
        when(caseEventMessageRepository.findByMessageIdToUpdate(anyString())).thenReturn(List.of(messageEntity));
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    public void should_handle_error_and_retry_update_with_new_state(MessageState newState) {
        when(messageEntity.getState()).thenReturn(MessageState.READY);

        updateRecordErrorHandlingService.handleUpdateError(newState, "mewssageId", 0, null);

        verify(caseEventMessageRepository).updateMessageState(newState, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"READY", "UNPROCESSABLE"})
    public void should_handle_error_and_update_READY_and_UNPROCESSABLE_to_PROCESSED(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(MessageState.PROCESSED, "mewssageId", 0, null);

        verify(caseEventMessageRepository).updateMessageState(MessageState.PROCESSED, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"READY", "UNPROCESSABLE"})
    public void should_handle_error_and_update_READY_and_UNPROCESSABLE_to_UNPROCESSABLE(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(MessageState.UNPROCESSABLE, "mewssageId", 0, null);

        verify(caseEventMessageRepository).updateMessageState(MessageState.UNPROCESSABLE, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    public void should_handle_error_and_should_not_update_if_message_already_processed(MessageState newState) {
        when(messageEntity.getState()).thenReturn(MessageState.PROCESSED);

        updateRecordErrorHandlingService.handleUpdateError(newState, "mewssageId", 0, null);

        verify(caseEventMessageRepository, never()).updateMessageState(any(MessageState.class), Mockito.<String>anyList());
    }

   @Test
    public void should_handle_error_and_update_retry_details() {
        when(messageEntity.getState()).thenReturn(MessageState.READY);

       LocalDateTime holdUntil = LocalDateTime.now();

        updateRecordErrorHandlingService.handleUpdateError(null, "mewssageId", 1, holdUntil);

        verify(caseEventMessageRepository).updateMessageWithRetryDetails(1, holdUntil, "mewssageId");
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    public void should_handle_error_and_should_not_update_retry_details_if_message_already_processed(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(null, "mewssageId", 1, LocalDateTime.now());

        verify(caseEventMessageRepository, never()).updateMessageWithRetryDetails(anyInt(), any(LocalDateTime.class), anyString());
    }
}

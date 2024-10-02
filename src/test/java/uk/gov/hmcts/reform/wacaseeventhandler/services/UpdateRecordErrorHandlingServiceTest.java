package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageErrorHandlingRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRecordErrorHandlingServiceTest {

    @Mock
    private CaseEventMessageErrorHandlingRepository errorHandlingRepository;

    @Mock
    private CaseEventMessageEntity messageEntity;

    @InjectMocks
    private UpdateRecordErrorHandlingService updateRecordErrorHandlingService;

    @BeforeEach
    void setUp() {
        when(errorHandlingRepository.findByMessageIdToUpdate(anyString())).thenReturn(List.of(messageEntity));
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    void should_handle_error_and_retry_update_with_new_state(MessageState newState) {
        when(messageEntity.getState()).thenReturn(MessageState.READY);

        updateRecordErrorHandlingService.handleUpdateError(newState, "mewssageId", 0, null);

        verify(errorHandlingRepository).updateMessageState(newState, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"READY", "UNPROCESSABLE"})
    void should_handle_error_and_update_Ready_and_Unprocessable_to_Proccessed(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(MessageState.PROCESSED, "mewssageId", 0, null);

        verify(errorHandlingRepository).updateMessageState(MessageState.PROCESSED, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"READY", "UNPROCESSABLE"})
    void should_handle_error_and_update_Ready_and_Unprocessable_to_Unprocessable(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(MessageState.UNPROCESSABLE, "mewssageId", 0, null);

        verify(errorHandlingRepository).updateMessageState(MessageState.UNPROCESSABLE, List.of("mewssageId"));
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    void should_handle_error_and_should_not_update_if_message_already_processed(MessageState newState) {
        when(messageEntity.getState()).thenReturn(MessageState.PROCESSED);

        updateRecordErrorHandlingService.handleUpdateError(newState, "mewssageId", 0, null);

        verify(errorHandlingRepository, never()).updateMessageState(any(MessageState.class), Mockito.<String>anyList());
    }

    @Test
    void should_handle_error_and_update_retry_details() {
        when(messageEntity.getState()).thenReturn(MessageState.READY);

        LocalDateTime holdUntil = LocalDateTime.now();

        updateRecordErrorHandlingService.handleUpdateError(null, "mewssageId", 1, holdUntil);

        verify(errorHandlingRepository).updateMessageWithRetryDetails(1, holdUntil, "mewssageId");
    }

    @ParameterizedTest
    @EnumSource(names = {"PROCESSED", "UNPROCESSABLE"})
    void should_handle_error_and_should_not_update_retry_if_message_already_processed(MessageState state) {
        when(messageEntity.getState()).thenReturn(state);

        updateRecordErrorHandlingService.handleUpdateError(null, "mewssageId", 1, LocalDateTime.now());

        verify(errorHandlingRepository, never())
            .updateMessageWithRetryDetails(anyInt(), any(LocalDateTime.class), anyString());
    }
}

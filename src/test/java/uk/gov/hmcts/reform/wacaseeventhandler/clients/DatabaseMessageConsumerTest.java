package uk.gov.hmcts.reform.wacaseeventhandler.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import feign.FeignException;
import feign.Request;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.UpdateRecordErrorHandlingService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.util.TestFixtures.createCaseEventMessage;

@ExtendWith(MockitoExtension.class)
class DatabaseMessageConsumerTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private CaseEventMessageMapper caseEventMessageMapper;

    @Mock
    private CcdEventProcessor ccdEventProcessor;

    @Mock
    private UpdateRecordErrorHandlingService updateRecordErrorHandlingService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @Mock
    private TelemetryContext telemetryContext;

    @Mock
    private OperationContext operationContext;

    @InjectMocks
    private DatabaseMessageConsumer databaseMessageConsumer;

    @Captor
    private ArgumentCaptor<LocalDateTime> holdUntilCaptor;

    @Captor
    private ArgumentCaptor<Integer> retryCountCaptor;

    @BeforeEach
    public void setup() {
        transactionTemplate.setTransactionManager(platformTransactionManager);
        lenient().when(telemetryContext.getOperation()).thenReturn(operationContext);
    }

    @NotNull
    private CaseEventMessageEntity createCaseEventMessageEntity() {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageContent("{\"UserId\" : \"databaseMessageConsumerTestUserId\"}");
        return caseEventMessageEntity;
    }

    @Test
    void should_not_process_message_if_null_message_selected() {
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(null);
        databaseMessageConsumer.run();
        verify(caseEventMessageMapper, never()).mapToCaseEventMessage(any());
        verifyNoInteractions(ccdEventProcessor);
    }

    @Test
    void should_process_message_if_message_selected() throws Exception {
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess())
            .thenReturn(caseEventMessageEntity);

        final CaseEventMessage caseEventMessage = createCaseEventMessage();
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);
        databaseMessageConsumer.run();
        verify(caseEventMessageMapper).mapToCaseEventMessage(caseEventMessageEntity);
        verify(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
    }

    @Test
    void should_process_message_and_set_as_unprocessable_if_non_retryable_feign_error_occurs() throws Exception {
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);

        final CaseEventMessage caseEventMessage = createCaseEventMessage();
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);

        final Request request = mock(Request.class);
        FeignException.BadRequest internalServerError = new FeignException.BadRequest(
            "Error Message",
            request,
            new byte[]{},
            Collections.emptyMap()
        );

        doThrow(internalServerError)
            .when(ccdEventProcessor).processMessage(caseEventMessage);
        databaseMessageConsumer.run();

        verify(caseEventMessageMapper).mapToCaseEventMessage(caseEventMessageEntity);
        verify(ccdEventProcessor).processMessage(caseEventMessage);
        verify(caseEventMessageRepository).updateMessageState(
            MessageState.UNPROCESSABLE,
            List.of(caseEventMessage.getMessageId())
        );
    }

    @Test
    void should_process_message_and_set_as_unprocessable_if_non_retryable_exception_occurs() throws Exception {
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);

        final CaseEventMessage caseEventMessage = createCaseEventMessage();
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);

        doThrow(mock(JsonProcessingException.class))
            .when(ccdEventProcessor).processMessage(caseEventMessage);
        databaseMessageConsumer.run();

        verify(caseEventMessageMapper).mapToCaseEventMessage(caseEventMessageEntity);
        verify(ccdEventProcessor).processMessage(caseEventMessage);
        verify(caseEventMessageRepository).updateMessageState(
            MessageState.UNPROCESSABLE,
            List.of(caseEventMessage.getMessageId())
        );
    }

    private static List<Arguments> getRetryableTestParameters() {
        return DatabaseMessageConsumer.RETRY_COUNT_TO_DELAY_MAP.entrySet().stream()
            .map(entrySet -> Arguments.of(entrySet.getKey(), entrySet.getValue()))
            .toList();
    }

    @ParameterizedTest
    @MethodSource("getRetryableTestParameters")
    void should_process_message_and_update_hold_until_and_retry_count_when_non_retryable_errors_occur(
        int retryCount, int holdUntilIncrement) throws Exception {

        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess())
            .thenReturn(createCaseEventMessageEntity());

        final CaseEventMessage caseEventMessage = createCaseEventMessage(retryCount - 1);
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);

        final Request request = mock(Request.class);
        FeignException.InternalServerError errorMessage = new FeignException.InternalServerError(
            "Error Message",
            request, new byte[]{},
            Collections.emptyMap()
        );

        doThrow(errorMessage)
            .when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        LocalDateTime now = LocalDateTime.now();

        databaseMessageConsumer.run();

        synchronized (this) {
            verify(caseEventMessageRepository)
                .updateMessageWithRetryDetails(retryCountCaptor.capture(), holdUntilCaptor.capture(), anyString());
            LocalDateTime updatedHoldUntilValue = holdUntilCaptor.getValue();
            assertTrue(updatedHoldUntilValue.isAfter(now));

            Assertions.assertThat(updatedHoldUntilValue.truncatedTo(ChronoUnit.SECONDS))
                .isCloseTo(now.plusSeconds(holdUntilIncrement).truncatedTo(ChronoUnit.SECONDS),
                           Assertions.within(1, ChronoUnit.SECONDS));
        }
        assertEquals(retryCount, retryCountCaptor.getValue());
    }

    @Test
    void should_process_message_and_update_to_unprocessable_when_retry_count_exceed_and_non_retryable_errors_occur()
        throws Exception {

        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess())
            .thenReturn(createCaseEventMessageEntity());

        final CaseEventMessage caseEventMessage = createCaseEventMessage(8);
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);

        final Request request = mock(Request.class);
        FeignException.NotFound errorMessage = new FeignException.NotFound(
            "Error Message",
            request, new byte[]{},
            Collections.emptyMap()
        );

        doThrow(errorMessage)
            .when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));

        databaseMessageConsumer.run();

        verify(ccdEventProcessor).processMessage(caseEventMessage);
        verify(caseEventMessageRepository).updateMessageState(
            MessageState.UNPROCESSABLE,
            List.of(caseEventMessage.getMessageId())
        );
    }

    @Test
    void should_process_message_and_set_as_processed() throws Exception {
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);

        final CaseEventMessage caseEventMessage = createCaseEventMessage();
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);

        databaseMessageConsumer.run();

        verify(caseEventMessageMapper).mapToCaseEventMessage(caseEventMessageEntity);
        verify(ccdEventProcessor).processMessage(caseEventMessage);
        verify(caseEventMessageRepository).updateMessageState(
            MessageState.PROCESSED,
            List.of(caseEventMessage.getMessageId())
        );
    }

    @Test
    void should_retry_to_update_record_when_update_state_failed() {
        final CaseEventMessage caseEventMessage = createCaseEventMessage();
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();

        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);
        String messageId = caseEventMessage.getMessageId();
        when(caseEventMessageRepository.updateMessageState(MessageState.PROCESSED,
                                                           List.of(messageId)))
            .thenThrow(new RuntimeException());

        when(platformTransactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionStatus).setRollbackOnly();

        databaseMessageConsumer.run();

        verify(updateRecordErrorHandlingService).handleUpdateError(MessageState.PROCESSED, messageId, 0, null);
    }

    @Test
    void should_retry_to_update_record_when_update_retry_details_failed() throws JsonProcessingException {
        final int retryCount = 2;
        final CaseEventMessage caseEventMessage = createCaseEventMessage(retryCount - 1);
        CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        String messageId = caseEventMessage.getMessageId();

        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);
        when(caseEventMessageMapper.mapToCaseEventMessage(any(CaseEventMessageEntity.class)))
            .thenReturn(caseEventMessage);
        when(caseEventMessageRepository.updateMessageWithRetryDetails(eq(retryCount), any(), eq(messageId)))
            .thenThrow(new RuntimeException());

        when(platformTransactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionStatus).setRollbackOnly();

        final Request request = mock(Request.class);
        FeignException.InternalServerError errorMessage = new FeignException.InternalServerError(
            "Error Message",
            request, new byte[]{},
            Collections.emptyMap()
        );

        doThrow(errorMessage)
            .when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));

        databaseMessageConsumer.run();

        verify(updateRecordErrorHandlingService).handleUpdateError(eq(null), eq(messageId), eq(retryCount), any());
    }
}

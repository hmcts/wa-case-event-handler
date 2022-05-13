package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class CancellationCaseEventHandlerTest {

    public static final String TENANT_ID = "ia";
    public static final String CANCEL_TASKS_MESSAGE_NAME = "cancelTasks";
    private static final String TASK_CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";
    private static final String SERVICE_AUTH_TOKEN = "s2s token";
    ListAppender<ILoggingEvent> listAppender;
    private EventInformation eventInformation;
    @Mock
    private WorkflowApiClient workflowApiClient;
    @Mock
    private AuthTokenGenerator serviceAuthGenerator;
    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;
    @InjectMocks
    private CancellationCaseEventHandler handlerService;

    @BeforeEach
    void setUp() {
        lenient().when(serviceAuthGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        eventInformation = EventInformation.builder()
            .eventId("some event id")
            .newStateId("some post state")
            .previousStateId("some previous state")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        Logger cancellationCaseEventHandlerLogger =
            (Logger) LoggerFactory.getLogger(CancellationCaseEventHandler.class);
        listAppender = new ListAppender<>();
        cancellationCaseEventHandlerLogger.addAppender(listAppender);
        listAppender.start();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Cancel"),
            null, null,
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInformation);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );

    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("UPDATE")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            null, null,
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_warning_text() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("UPDATE")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            null, dmnStringValue("warningText"),
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored",
            logsList.get(0).getMessage()
        );
        assertEquals(Level.WARN, logsList.get(0).getLevel());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_warning_code() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("UPDATE")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            dmnStringValue("warningCode"), null,
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored",
            logsList.get(0).getMessage()
        );
        assertEquals(Level.WARN, logsList.get(0).getLevel());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_process_category() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("UPDATE")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            null, null,
            null,
            dmnStringValue("processCategory")
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored",
            logsList.get(0).getMessage()
        );
        assertEquals(Level.WARN, logsList.get(0).getLevel());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_empty_results() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest();

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse(Collections.emptyList()));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInformation);

        assertThat(actualResponse).isEmpty();

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );

    }

    @Test
    void should_be_able_to_handle() {
        CancellationEvaluateResponse result1 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Cancel"))
            .processCategories(dmnStringValue("some category"))
            .build();

        CancellationEvaluateResponse result2 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Cancel"))
            .processCategories(dmnStringValue("some other category"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(2))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category")
        );

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(1),
            "some case reference",
            dmnStringValue("some other category")
        );

    }

    @Test
    void should_be_able_to_handle_with_multiple_categories() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Cancel"))
            .processCategories(dmnStringValue("category1, category2"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("category1, category2")
        );

    }

    @Test
    void should_be_able_to_handle_with_no_categories() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Cancel"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        // Because of backwards compatibility 2 messages will be created but they are identical hence only sending once.
        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequest(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            null
        );
    }

    @Test
    void should_ignore_invalid_instances() {
        InitiateEvaluateResponse invalidInstanceResults = InitiateEvaluateResponse.builder().build();

        List<InitiateEvaluateResponse> results = List.of(invalidInstanceResults);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(0))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), any());
    }

    @Test
    void should_filter_out_non_cancellations() {
        CancellationEvaluateResponse warningResult = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("category"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(warningResult);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(0))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), any());
    }

    private EvaluateDmnRequest buildEvaluateDmnRequest() {
        Map<String, DmnValue<?>> variables = Map.of(
            "event", dmnStringValue("some event id"),
            "state", dmnStringValue("some post state"),
            "fromState", dmnStringValue("some previous state")
        );

        return new EvaluateDmnRequest(variables);
    }

    private EvaluateDmnRequest buildEvaluateUpdateDmnRequest() {
        Map<String, DmnValue<?>> variables = Map.of(
            "event", dmnStringValue("UPDATE"),
            "state", dmnStringValue(""),
            "fromState", dmnStringValue("")
        );

        return new EvaluateDmnRequest(variables);
    }

    private void assertSendMessageRequestOldFormat(
        SendMessageRequest sendMessageRequest,
        String caseReference,
        DmnValue<String> categories
    ) {

        Map<String, DmnValue<?>> expectedCorrelationKeys = new HashMap<>();
        expectedCorrelationKeys.put("caseId", dmnStringValue(caseReference));
        if (categories != null && categories.getValue() != null) {
            expectedCorrelationKeys.put("taskCategory", categories);
        }

        assertThat(sendMessageRequest.getMessageName()).isEqualTo(CANCEL_TASKS_MESSAGE_NAME);
        assertThat(sendMessageRequest.getCorrelationKeys()).isEqualTo(expectedCorrelationKeys);
        assertTrue(sendMessageRequest.isAll());
    }

    private void assertSendMessageRequest(
        SendMessageRequest sendMessageRequest,
        String caseReference,
        DmnValue<String> categories
    ) {

        Map<String, DmnValue<?>> expectedCorrelationKeys = new HashMap<>();
        expectedCorrelationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(categories.getValue().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            categoriesToCancel.forEach(category ->
                expectedCorrelationKeys.put("__processCategory__" + category, dmnBooleanValue(true))
            );
        }


        assertThat(sendMessageRequest.getMessageName()).isEqualTo(CANCEL_TASKS_MESSAGE_NAME);
        assertThat(sendMessageRequest.getCorrelationKeys()).isEqualTo(expectedCorrelationKeys);
        assertTrue(sendMessageRequest.isAll());
    }
}

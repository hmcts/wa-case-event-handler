package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.Warning;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
class WarningCaseEventHandlerTest {


    public static final String TENANT_ID = "ia";
    public static final String WARN_TASKS_MESSAGE_NAME = "warnProcess";
    private static final String TASK_CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";
    private static final String SERVICE_AUTH_TOKEN = "s2s token";
    public static final String WARNING_LIST = "warningsToAdd";
    private EventInformation eventInformation;
    @Mock
    private WorkflowApiClient workflowApiClient;
    @Mock
    private AuthTokenGenerator serviceAuthGenerator;
    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;
    @InjectMocks
    private WarningCaseEventHandler handlerService;


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
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Warn"), dmnStringValue("Code1"), dmnStringValue("Text1"),
            null,
            null
        ), new CancellationEvaluateResponse(
            dmnStringValue("Warn"), dmnStringValue("Code2"), dmnStringValue("Text2"),
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
    void should_be_able_to_handle_with_different_categories_and_warnings() {
        CancellationEvaluateResponse result1 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        CancellationEvaluateResponse result2 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some other category"))
            .warningCode(dmnStringValue("Code2"))
            .warningText(dmnStringValue("Text2"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(2))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category"),
            "Code1", "Text1"
        );

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(1),
            "some case reference",
            dmnStringValue("some other category"),
            "Code2", "Text2"
        );
    }

    @Test
    void should_be_able_to_handle_with_multiple_same_categories_and_warnings() {
        CancellationEvaluateResponse result1 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        CancellationEvaluateResponse result2 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category"),
            "Code1", "Text1"
        );
    }

    @Test
    void should_be_able_to_handle_with_multiple_same_warnings_on_different_categories() {
        CancellationEvaluateResponse result1 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        CancellationEvaluateResponse result2 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some other category"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(2))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category"),
            "Code1", "Text1"
        );

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(1),
            "some case reference",
            dmnStringValue("some other category"),
            "Code1", "Text1"
        );
    }

    @Test
    void should_be_able_to_handle_with_warnings_and_no_categories() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .warningCode(dmnStringValue("Code1"))
            .warningText(dmnStringValue("Text1"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        // Because of backwards compatibility 2 messages will be created but they are identical hence only sending once.
        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            null,
            "Code1", "Text1"
        );
    }

    @Test
    void should_be_able_to_handle_with_categories_and_with_no_warnings() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithoutWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category")
        );
    }

    @Test
    void should_be_able_to_handle_with_categories_and_with_no_warning_code() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningText(dmnStringValue("Text1"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithoutWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category")
        );
    }

    @Test
    void should_be_able_to_handle_with_categories_and_with_no_warning_text() {
        CancellationEvaluateResponse result = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("some category"))
            .warningCode(dmnStringValue("Code1"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result);

        handlerService.handle(results, eventInformation);

        verify(workflowApiClient, times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertSendMessageRequestWithoutWarnings(
            sendMessageRequestCaptor.getAllValues().get(0),
            "some case reference",
            dmnStringValue("some category")
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
    void should_filter_out_non_warnings() {
        CancellationEvaluateResponse warningResult = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Cancel"))
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

    private void assertSendMessageRequestWithWarnings(
        SendMessageRequest sendMessageRequest,
        String caseReference,
        DmnValue<String> categories,
        String warningCode,
        String warningText) {

        Map<String, DmnValue<?>> expectedCorrelationKeys = getCorrelatedKeyMap(caseReference, categories);

        assertThat(sendMessageRequest.getMessageName()).isEqualTo(WARN_TASKS_MESSAGE_NAME);
        assertThat(sendMessageRequest.getCorrelationKeys()).isEqualTo(expectedCorrelationKeys);
        assertTrue(sendMessageRequest.isAll());
        assertTrue(sendMessageRequest.getProcessVariables().containsKey(WARNING_LIST));
        final DmnValue<?> warnings = sendMessageRequest.getProcessVariables().get(WARNING_LIST);

        assertTrue(warnings != null);

        final String warningAsString = (String) warnings.getValue();
        WarningValues warningValues = new WarningValues(warningAsString);

        assertThat(warningValues.getValues().contains(new Warning(warningCode, warningText)));
    }

    @NotNull
    private Map<String, DmnValue<?>> getCorrelatedKeyMap(String caseReference, DmnValue<String> categories) {
        Map<String, DmnValue<?>> expectedCorrelationKeys = new HashMap<>();
        expectedCorrelationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(categories.getValue().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            categoriesToCancel.forEach(category ->
                                           expectedCorrelationKeys.put(
                                               "__processCategory__" + category,
                                               dmnBooleanValue(true)
                                           )
            );
        }
        return expectedCorrelationKeys;
    }

    private void assertSendMessageRequestWithoutWarnings(
        SendMessageRequest sendMessageRequest,
        String caseReference,
        DmnValue<String> categories) {

        Map<String, DmnValue<?>> expectedCorrelationKeys = getCorrelatedKeyMap(caseReference, categories);

        assertThat(sendMessageRequest.getMessageName()).isEqualTo(WARN_TASKS_MESSAGE_NAME);
        assertThat(sendMessageRequest.getCorrelationKeys()).isEqualTo(expectedCorrelationKeys);
        assertTrue(sendMessageRequest.isAll());
        assertNull(sendMessageRequest.getProcessVariables());
    }

}

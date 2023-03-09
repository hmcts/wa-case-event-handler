package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.TaskManagementApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskOperationRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@ExtendWith(MockitoExtension.class)
class ReconfigurationCaseEventHandlerTest {

    public static final String TENANT_ID = "ia";
    private static final String TASK_CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";
    private static final String SERVICE_AUTH_TOKEN = "s2s token";
    ListAppender<ILoggingEvent> listAppender;
    private EventInformation eventInformation;
    @Mock
    private WorkflowApiClient workflowApiClient;
    @Mock
    private TaskManagementApiClient taskManagementApiClient;
    @Mock
    private AuthTokenGenerator serviceAuthGenerator;
    @InjectMocks
    private ReconfigurationCaseEventHandler handlerService;

    @BeforeEach
    void setUp() {
        lenient().when(serviceAuthGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        eventInformation = EventInformation.builder()
            .eventId("ANY_EVENT")
            .newStateId("some post state")
            .previousStateId("some previous state")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        Logger reconfigureCaseEventHandlerLogger =
            (Logger) LoggerFactory.getLogger(ReconfigurationCaseEventHandler.class);
        listAppender = new ListAppender<>();
        reconfigureCaseEventHandlerLogger.addAppender(listAppender);
        listAppender.start();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest();

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
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInformation);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(3, logsList.size());
        logsList.clear();

    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_null_fields() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
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
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(3, logsList.size());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_warning_text() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
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
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(actualResponse, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(log -> log.getMessage().equals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored"
        )));

        assertTrue(logsList.stream().anyMatch(log -> Level.WARN.equals(log.getLevel())));
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_warning_code() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
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
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(actualResponse, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(log -> log.getMessage().equals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored"
        )));

        assertTrue(logsList.stream().anyMatch(log -> Level.WARN.equals(log.getLevel())));
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_nonnull_process_category() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
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
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        verify(workflowApiClient, times(1)).evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        );

        handlerService.handle(actualResponse, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream().anyMatch(log -> log.getMessage().equals(
            "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored"
        )));

        assertTrue(logsList.stream().anyMatch(log -> Level.WARN.equals(log.getLevel())));
        logsList.clear();

    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_blank_warning_text() {

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            null,
            dmnStringValue(""),
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(3, logsList.size());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_blank_warning_code() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
            .newStateId("")
            .previousStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.now())
            .build();

        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Reconfigure"),
            dmnStringValue(""), null,
            null,
            null
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(3, logsList.size());
        logsList.clear();
    }

    @Test
    void should_evaluate_the_dmn_table_and_return_results_for_reconfigure_action_with_blank_process_category() {
        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateUpdateDmnRequest();
        EventInformation eventInfo = EventInformation.builder()
            .eventId("ANY_EVENT")
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
            dmnStringValue("")
        ));

        when(workflowApiClient.evaluateCancellationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_CANCELLATION_DMN_TABLE,
            TENANT_ID,
            evaluateDmnRequest
        )).thenReturn(new EvaluateDmnResponse<>(results));

        List<? extends EvaluateResponse> actualResponse = handlerService.evaluateDmn(eventInfo);

        assertThat(actualResponse).isSameAs(results);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(1)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(3, logsList.size());
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
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

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
            .action(dmnStringValue("Reconfigure"))
            .build();

        CancellationEvaluateResponse result2 = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Reconfigure"))
            .processCategories(dmnStringValue("some other category"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(result1, result2);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(2)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );

    }


    @Test
    void should_ignore_invalid_instances() {
        InitiateEvaluateResponse invalidInstanceResults = InitiateEvaluateResponse.builder().build();

        List<InitiateEvaluateResponse> results = List.of(invalidInstanceResults);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(0)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );
    }

    @Test
    void should_filter_out_non_cancellations() {
        CancellationEvaluateResponse warningResult = CancellationEvaluateResponse.builder()
            .action(dmnStringValue("Warn"))
            .processCategories(dmnStringValue("category"))
            .build();

        List<CancellationEvaluateResponse> results = List.of(warningResult);

        handlerService.handle(results, eventInformation);

        verify(taskManagementApiClient, times(0)).performOperation(
            anyString(),
            any(TaskOperationRequest.class)
        );
    }

    private EvaluateDmnRequest buildEvaluateDmnRequest() {
        Map<String, DmnValue<?>> variables = Map.of(
            "event", dmnStringValue("ANY_EVENT"),
            "state", dmnStringValue("some post state"),
            "fromState", dmnStringValue("some previous state")
        );

        return new EvaluateDmnRequest(variables);
    }

    private EvaluateDmnRequest buildEvaluateUpdateDmnRequest() {
        Map<String, DmnValue<?>> variables = Map.of(
            "event", dmnStringValue("ANY_EVENT"),
            "state", dmnStringValue(""),
            "fromState", dmnStringValue("")
        );

        return new EvaluateDmnRequest(variables);
    }

}

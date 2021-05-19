package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.CancellationActions;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_CANCELLATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@Service
@Order(1)
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UseConcurrentHashMap"})
public class CancellationCaseEventHandler implements CaseEventHandler {

    private final AuthTokenGenerator serviceAuthGenerator;
    private final WorkflowApiClient workflowApiClient;

    public CancellationCaseEventHandler(AuthTokenGenerator serviceAuthGenerator, WorkflowApiClient workflowApiClient) {
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.workflowApiClient = workflowApiClient;
    }

    @Override
    public List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_CANCELLATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        String tenantId = eventInformation.getJurisdictionId().toLowerCase(Locale.ENGLISH);

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest(
            eventInformation.getPreviousStateId(),
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        EvaluateDmnResponse<? extends EvaluateResponse> response = workflowApiClient.evaluateDmn(
            serviceAuthGenerator.generate(),
            tableKey,
            tenantId,
            evaluateDmnRequest);

        return response.getResults();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(CancellationEvaluateResponse.class::isInstance)
            .map(CancellationEvaluateResponse.class::cast)
            .filter(result -> CancellationActions.CANCEL == CancellationActions.from(result.getAction().getValue()))
            .forEach(cancellationEvaluateResponse -> {
                DmnValue<String> taskCategories = cancellationEvaluateResponse.getProcessCategories();
                sendCancellationMessage(
                    eventInformation.getCaseId(),
                    taskCategories
                );
            });
    }

    private EvaluateDmnRequest buildEvaluateDmnRequest(
        String previousStateId,
        String eventId,
        String newStateId
    ) {
        Map<String, DmnValue<?>> variables = Map.of(
            "event", dmnStringValue(eventId),
            "state", dmnStringValue(newStateId),
            "fromState", dmnStringValue(previousStateId)
        );

        return new EvaluateDmnRequest(variables);
    }

    private void sendCancellationMessage(String caseReference, DmnValue<String> categories) {
        Set<SendMessageRequest> cancellationMessageRequests =
            buildCancellationMessageRequest(caseReference, categories);

        cancellationMessageRequests.forEach(message ->
            workflowApiClient.sendMessage(serviceAuthGenerator.generate(), message)
        );

    }

    private Set<SendMessageRequest> buildCancellationMessageRequest(
        String caseReference,
        DmnValue<String> categories
    ) {

        SendMessageRequest oldFormatCancellationMessage = createOldFormatCancellationMessage(caseReference, categories);
        SendMessageRequest cancellationMessage = createCancellationMessage(caseReference, categories);

        return new HashSet<>(asList(oldFormatCancellationMessage, cancellationMessage));
    }

    /**
     * This method creates a SendMessageRequest to cancel all tasks matching a set of correlation keys.
     * This method supports multiple categories.
     *
     * @param caseReference the case id to be used as a correlation key
     * @param categories    the categories to be used as correlation keys
     * @return The message request object.
     */
    private SendMessageRequest createCancellationMessage(String caseReference, DmnValue<String> categories) {

        Map<String, DmnValue<?>> correlationKeys = new HashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(categories.getValue().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            categoriesToCancel.forEach(category ->
                correlationKeys.put("__processCategory__" + category, dmnBooleanValue(true))
            );
        }

        return SendMessageRequest.builder()
            .messageName(TASK_CANCELLATION.getMessageName())
            .correlationKeys(correlationKeys)
            .all(true)
            .build();

    }

    /**
     * This method adds backwards support with current implementation which supports only single categories.
     * This method is deprecated and should not be used.
     *
     * @param caseReference the case id to be used as a correlation key
     * @param categories    the categories to be used as correlation keys
     * @return The message request object.
     */
    @Deprecated
    private SendMessageRequest createOldFormatCancellationMessage(String caseReference, DmnValue<String> categories) {

        Map<String, DmnValue<?>> correlationKeys = new HashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            correlationKeys.put("taskCategory", categories);
        }

        return SendMessageRequest.builder()
            .messageName(TASK_CANCELLATION.getMessageName())
            .correlationKeys(correlationKeys)
            .all(true)
            .build();

    }

}

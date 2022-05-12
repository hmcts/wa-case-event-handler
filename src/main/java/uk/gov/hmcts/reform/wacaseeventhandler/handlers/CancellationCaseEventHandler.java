package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import lombok.extern.slf4j.Slf4j;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_CANCELLATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@Slf4j
@Service
@Order(1)
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "unchecked"})
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

        String tenantId = eventInformation.getJurisdictionId();

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest(
            eventInformation.getPreviousStateId(),
            eventInformation.getEventId(),
            eventInformation.getNewStateId()
        );

        EvaluateDmnResponse<CancellationEvaluateResponse> response = workflowApiClient.evaluateCancellationDmn(
            serviceAuthGenerator.generate(),
            tableKey,
            tenantId,
            evaluateDmnRequest);

        List<CancellationEvaluateResponse> results = response.getResults();
        evaluateDmnResponse(eventInformation, results);
        return results;
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(CancellationEvaluateResponse.class::isInstance)
            .map(CancellationEvaluateResponse.class::cast)
            .filter(result -> CancellationActions.CANCEL == CancellationActions.from(result.getAction().getValue()))
            .forEach(cancellationEvaluateResponse -> {
                DmnValue<String> taskCategories = cancellationEvaluateResponse.getTaskCategories();
                DmnValue<String> processCategories = cancellationEvaluateResponse.getProcessCategories();
                sendCancellationMessage(
                    eventInformation.getCaseId(),
                    taskCategories,
                    processCategories
                );
            });
    }

    private void evaluateDmnResponse(EventInformation eventInformation, List<CancellationEvaluateResponse> results) {
        results.stream()
            .forEach(response -> evaluateReconfigureActionResponse(eventInformation.getEventId(), response));
    }

    private void evaluateReconfigureActionResponse(String eventId, CancellationEvaluateResponse response) {
        if (response.getAction().getValue().equals("Reconfigure")
            && eventId.equals("UPDATE")
            && (response.getWarningCode() != null
                && isNotBlank(response.getWarningCode().getValue())
                || response.getWarningText() != null
                   && isNotBlank(response.getWarningText().getValue())
                || response.getProcessCategories() != null
                   && isNotBlank(response.getProcessCategories().getValue()))) {
            log.warn(
                "DMN configuration has provided fields not suitable for Reconfiguration and that they will be ignored"
            );
        }
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

    private void sendCancellationMessage(String caseReference,
                                         DmnValue<String> categories,
                                         DmnValue<String> processCategories) {
        Set<SendMessageRequest> cancellationMessageRequests =
            buildCancellationMessageRequest(caseReference, categories, processCategories);

        cancellationMessageRequests.forEach(message -> {
                if (message != null) {
                    workflowApiClient.sendMessage(serviceAuthGenerator.generate(), message);
                }
            }
        );

    }

    private Set<SendMessageRequest> buildCancellationMessageRequest(
        String caseReference,
        DmnValue<String> categories,
        DmnValue<String> processCategories
    ) {

        SendMessageRequest oldFormatCancellationMessage =
            createOldFormatCancellationMessage(caseReference, categories, processCategories);
        SendMessageRequest cancellationMessage =
            createCancellationMessage(caseReference, categories, processCategories);

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
    private SendMessageRequest createCancellationMessage(String caseReference,
                                                         DmnValue<String> categories,
                                                         DmnValue<String> processCategories) {

        Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(categories.getValue().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            categoriesToCancel.forEach(cat ->
                correlationKeys.put("__processCategory__" + cat, dmnBooleanValue(true))
            );
        }

        if (processCategories != null && processCategories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(processCategories.getValue().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            categoriesToCancel.forEach(cat ->
                correlationKeys.put("__processCategory__" + cat, dmnBooleanValue(true))
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
     * @deprecated part of the old implementation with no support for multiple categories
     */
    @Deprecated(since = "1.1")
    private SendMessageRequest createOldFormatCancellationMessage(String caseReference,
                                                                  DmnValue<String> categories,
                                                                  DmnValue<String> processCategories) {

        if (processCategories == null) {
            Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
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
        return null;
    }

}

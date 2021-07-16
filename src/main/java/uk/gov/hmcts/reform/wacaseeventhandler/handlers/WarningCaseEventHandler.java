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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_WARN;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@Service
@Order(2)
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "unchecked"})
public class WarningCaseEventHandler implements CaseEventHandler {

    private final AuthTokenGenerator serviceAuthGenerator;
    private final WorkflowApiClient workflowApiClient;

    public WarningCaseEventHandler(AuthTokenGenerator serviceAuthGenerator, WorkflowApiClient workflowApiClient) {
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.workflowApiClient = workflowApiClient;
    }

    @Override
    public List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_WARN.getTableKey(
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

        return response.getResults();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(CancellationEvaluateResponse.class::isInstance)
            .map(CancellationEvaluateResponse.class::cast)
            .filter(result -> CancellationActions.WARN == CancellationActions.from(result.getAction().getValue()))
            .forEach(cancellationEvaluateResponse -> {
                DmnValue<String> taskCategories = cancellationEvaluateResponse.getTaskCategories();
                DmnValue<String> processCategories = cancellationEvaluateResponse.getProcessCategories();
                sendWarningMessage(
                    eventInformation.getCaseId(),
                    taskCategories,
                    processCategories,
                    cancellationEvaluateResponse.getWarningCode(),
                    cancellationEvaluateResponse.getWarningText()
                );
            });

    }

    private void sendWarningMessage(String caseReference,
                                    DmnValue<String> categories,
                                    DmnValue<String> processCategories,
                                    DmnValue<String> warningCode,
                                    DmnValue<String> warningText) {
        Set<SendMessageRequest> warningMessageRequest =
            buildWarningMessageRequest(caseReference, categories,
                                       processCategories, warningCode, warningText);

        warningMessageRequest.forEach(message -> {
                if (message != null) {
                    workflowApiClient.sendMessage(serviceAuthGenerator.generate(), message);
                }
            }

        );
    }

    private Set<SendMessageRequest> buildWarningMessageRequest(
        String caseReference,
        DmnValue<String> categories,
        DmnValue<String> processCategories,
        DmnValue<String> warningCode,
        DmnValue<String> warningText
    ) {

        SendMessageRequest oldFormatWarningMessage =
            createOldFormatWarningMessage(caseReference, categories, processCategories,
                                          warningCode, warningText);
        SendMessageRequest warningMessage =
            createWarningMessage(caseReference, categories, processCategories,
                                 warningCode, warningText);

        return new HashSet<>(asList(oldFormatWarningMessage, warningMessage));
    }

    /**
     * This method creates a SendMessageRequest to warn all tasks matching a set of correlation keys.
     * This method supports multiple categories.
     *
     * @param caseReference the case id to be used as a correlation key
     * @param categories    the categories to be used as correlation keys
     * @return The message request object.
     */
    private SendMessageRequest createWarningMessage(String caseReference,
                                                    DmnValue<String> categories,
                                                    DmnValue<String> processCategories,
                                                    DmnValue<String> warningCode,
                                                    DmnValue<String> warningText) {

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

        if (checkForWarningProperties(warningCode, warningText)) {
            return addWarningsToProcessVariables(correlationKeys, warningCode, warningText);
        }

        return SendMessageRequest.builder()
            .messageName(TASK_WARN.getMessageName())
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
    private SendMessageRequest createOldFormatWarningMessage(String caseReference,
                                                             DmnValue<String> categories,
                                                             DmnValue<String> processCategories,
                                                             DmnValue<String> warningCode,
                                                             DmnValue<String> warningText) {

        if (processCategories == null) {
            Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
            correlationKeys.put("caseId", dmnStringValue(caseReference));

            if (categories != null && categories.getValue() != null) {
                correlationKeys.put("taskCategory", categories);
            }

            if (checkForWarningProperties(warningCode, warningText)) {
                return addWarningsToProcessVariables(correlationKeys, warningCode, warningText);
            }
            return SendMessageRequest.builder()
                .messageName(TASK_WARN.getMessageName())
                .correlationKeys(correlationKeys)
                .all(true)
                .build();

        }
        return null;

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

    private boolean checkForWarningProperties(
        DmnValue<String> warningCode, DmnValue<String> warningText) {
        return warningCode != null && warningText != null;
    }

    private SendMessageRequest addWarningsToProcessVariables(
        Map<String, DmnValue<?>> correlationKeys,
        DmnValue<String> warningCode,
        DmnValue<String> warningText) {

        Map<String, DmnValue<?>> processVariables = Map.of(
            "warningCode", warningCode,
            "warningText", warningText
        );

        return SendMessageRequest.builder()
            .messageName(TASK_WARN.getMessageName())
            .correlationKeys(correlationKeys)
            .processVariables(processVariables)
            .all(true)
            .build();
    }
}


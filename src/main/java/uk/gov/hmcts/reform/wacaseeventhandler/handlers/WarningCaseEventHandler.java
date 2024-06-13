package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.CancellationActions;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.Warning;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_WARN;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@Service
@Order(2)
@Slf4j
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
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

    @SuppressWarnings("PMD.ConfusingTernary")
    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        log.info("WarningCaseEventHandler eventInformation:{}", eventInformation);
        Set<CancellationEvaluateResponse> emptyWarnings = new LinkedHashSet<>();
        Set<CancellationEvaluateResponse> ctgWarnings = new LinkedHashSet<>();
        Set<Warning> warnings = new LinkedHashSet<>();

        results.stream()
            .filter(CancellationEvaluateResponse.class::isInstance)
            .map(CancellationEvaluateResponse.class::cast)
            .filter(result -> CancellationActions.WARN == CancellationActions.from(result.getAction().getValue()))
            .forEach(warnResponse -> {
                if (ObjectUtils.isEmpty(warnResponse.getWarningCode())
                    || ObjectUtils.isEmpty(warnResponse.getWarningText())) {
                    emptyWarnings.add(warnResponse);
                } else {
                    if (!ObjectUtils.isEmpty(warnResponse.getProcessCategories())
                        || !ObjectUtils.isEmpty(warnResponse.getTaskCategories())) {
                        CancellationEvaluateResponse localWarnResponse = CancellationEvaluateResponse.builder()
                            .processCategories(warnResponse.getProcessCategories())
                            .taskCategories(warnResponse.getTaskCategories())
                            .warningText(warnResponse.getWarningText())
                            .warningCode(warnResponse.getWarningCode()).build();
                        ctgWarnings.add(localWarnResponse);
                    } else {
                        final Warning warning = new Warning(
                            warnResponse.getWarningCode().getValue(),
                            warnResponse.getWarningText().getValue()
                        );
                        warnings.add(warning);
                    }
                }
            });

        processWarningResponse(emptyWarnings, warnings, ctgWarnings, eventInformation);
    }

    private void processWarningResponse(
        Set<CancellationEvaluateResponse> emptyWarnings,
        Set<Warning> warnings,
        Set<CancellationEvaluateResponse> ctgWarnings,
        EventInformation eventInformation

    ) {
        // scenario: event without warning attributes
        if (!emptyWarnings.isEmpty()) {
            emptyWarnings.forEach(response -> sendWarningMessage(
                eventInformation.getCaseId(),
                response.getTaskCategories(),
                response.getProcessCategories(),
                null)
            );
        }

        // scenario: event without categories. Should contain unique warning attributes
        if (!warnings.isEmpty()) {
            WarningValues warningValues = new WarningValues();
            warnings.forEach(warning -> warningValues.getValues().add(warning));

            sendWarningMessage(
                eventInformation.getCaseId(),
                null,
                null,
                warningValues.getValuesAsJson()
            );
        }
        // scenario: event with categories and warning attributes
        if (!ctgWarnings.isEmpty()) {
            ctgWarnings.forEach(response -> {
                final Warning warning = new Warning(response.getWarningCode().getValue(),
                                                    response.getWarningText().getValue());

                WarningValues warningValues = new WarningValues();
                warningValues.getValues().add(warning);

                sendWarningMessage(
                    eventInformation.getCaseId(),
                    response.getTaskCategories(),
                    response.getProcessCategories(),
                    warningValues.getValuesAsJson()
                );
            });
        }
    }

    private void sendWarningMessage(String caseReference,
                                    DmnValue<String> categories,
                                    DmnValue<String> processCategories,
                                    String warningVariables) {
        Set<SendMessageRequest> warningMessageRequest =
            buildWarningMessageRequest(caseReference, categories,
                                       processCategories, warningVariables);

        warningMessageRequest.forEach(message -> {
                if (message != null) {
                    log.info("sendWarningMessage message:{}", message);
                    workflowApiClient.sendMessage(serviceAuthGenerator.generate(), message);
                }
            }
        );
    }

    private Set<SendMessageRequest> buildWarningMessageRequest(
        String caseReference,
        DmnValue<String> categories,
        DmnValue<String> processCategories,
        String warningVariables
    ) {

        SendMessageRequest oldFormatWarningMessage =
            createOldFormatWarningMessage(caseReference, categories, processCategories, warningVariables);
        SendMessageRequest warningMessage =
            createWarningMessage(caseReference, categories, processCategories, warningVariables);

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
                                                    String warningVariables) {

        Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
        correlationKeys.put("caseId", dmnStringValue(caseReference));

        if (categories != null && categories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(categories.getValue().split(","))
                .map(String::trim)
                .toList();

            categoriesToCancel.forEach(cat ->
                correlationKeys.put("__processCategory__" + cat, dmnBooleanValue(true))
            );
        }

        if (processCategories != null && processCategories.getValue() != null) {
            List<String> categoriesToCancel = Stream.of(processCategories.getValue().split(","))
                .map(String::trim)
                .toList();

            categoriesToCancel.forEach(cat ->
                correlationKeys.put("__processCategory__" + cat, dmnBooleanValue(true))
            );
        }

        if (!ObjectUtils.isEmpty(warningVariables)) {
            return addWarningsToProcessVariables(correlationKeys, warningVariables);
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
                                                             String warningVariables) {

        if (processCategories == null) {
            Map<String, DmnValue<?>> correlationKeys = new ConcurrentHashMap<>();
            correlationKeys.put("caseId", dmnStringValue(caseReference));

            if (categories != null && categories.getValue() != null) {
                correlationKeys.put("taskCategory", categories);
            }

            if (!ObjectUtils.isEmpty(warningVariables)) {
                return addWarningsToProcessVariables(correlationKeys, warningVariables);
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

    private SendMessageRequest addWarningsToProcessVariables(
        Map<String, DmnValue<?>> correlationKeys,
        String warningVariables) {

        return SendMessageRequest.builder()
            .messageName(TASK_WARN.getMessageName())
            .correlationKeys(correlationKeys)
            .processVariables(Map.of("warningsToAdd", new DmnValue<>(warningVariables, "String")))
            .all(true)
            .build();
    }
}


package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.Warning;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
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

public class NonIacWarningMessage {

    private final WorkflowApiClient workflowApiClient;
    private final AuthTokenGenerator serviceAuthGenerator;
    private final EventInformation eventInformation;

    public NonIacWarningMessage(WorkflowApiClient workflowApiClient,
                                AuthTokenGenerator serviceAuthGenerator,
                                EventInformation eventInformation) {
        this.workflowApiClient = workflowApiClient;
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.eventInformation = eventInformation;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void processWarningResponse(
        Set<CancellationEvaluateResponse> emptyWarnings,
        Set<Warning> warnings,
        Set<CancellationEvaluateResponse> ctgWarnings) {

        // scenario: event without warning attributes
        if (!emptyWarnings.isEmpty()) {
            emptyWarnings.forEach(response ->
                                      sendWarningMessage(
                                          eventInformation.getCaseId(),
                                          response.getTaskCategories(),
                                          response.getProcessCategories(),
                                          null
                                      )
            );
        }

        // scenario: event without categories. Should contain unique warning attributes
        if (!warnings.isEmpty()) {
            WarningValues warningValues = new WarningValues();
            warnings.stream().forEach(warning -> warningValues.getValues().add(warning));

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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void sendWarningMessage(String caseReference,
                                    DmnValue<String> categories,
                                    DmnValue<String> processCategories,
                                    String warningVariables) {
        Set<SendMessageRequest> warningMessageRequest =
            buildWarningMessageRequest(caseReference, categories,
                                       processCategories, warningVariables);

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

        if (!StringUtils.isEmpty(warningVariables)) {
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

            if (!StringUtils.isEmpty(warningVariables)) {
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

    private SendMessageRequest addWarningsToProcessVariables(
        Map<String, DmnValue<?>> correlationKeys,
        String warningVariables) {

        return SendMessageRequest.builder()
            .messageName(TASK_WARN.getMessageName())
            .correlationKeys(correlationKeys)
            .processVariables(Map.of("warnings", new DmnValue<>(warningVariables, "String")))
            .all(true)
            .build();
    }

}

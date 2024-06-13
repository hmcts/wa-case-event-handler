package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.TaskManagementApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.CancellationActions;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskFilter;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskFilterOperator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskOperation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskOperationName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskOperationRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_CANCELLATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@Slf4j
@Service
@Order(3)
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class ReconfigurationCaseEventHandler implements CaseEventHandler {

    private final AuthTokenGenerator serviceAuthGenerator;
    private final WorkflowApiClient workflowApiClient;
    private final TaskManagementApiClient taskManagementApiClient;

    public ReconfigurationCaseEventHandler(AuthTokenGenerator serviceAuthGenerator, WorkflowApiClient workflowApiClient,
                                           TaskManagementApiClient taskManagementApiClient) {
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.workflowApiClient = workflowApiClient;
        this.taskManagementApiClient = taskManagementApiClient;
    }

    @Override
    public List<? extends EvaluateResponse> evaluateDmn(EventInformation reconfigurationEventInformation) {
        String tableKey = TASK_CANCELLATION.getTableKey(
            reconfigurationEventInformation.getJurisdictionId(),
            reconfigurationEventInformation.getCaseTypeId()
        );
        log.debug("tableKey : {}", tableKey);
        String tenantId = reconfigurationEventInformation.getJurisdictionId();

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest(
            reconfigurationEventInformation.getPreviousStateId(),
            reconfigurationEventInformation.getEventId(),
            reconfigurationEventInformation.getNewStateId()
        );

        EvaluateDmnResponse<CancellationEvaluateResponse> response = workflowApiClient.evaluateCancellationDmn(
            serviceAuthGenerator.generate(),
            tableKey,
            tenantId,
            evaluateDmnRequest);

        return response.getResults();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation reconfigurationEventInformation) {
        log.info("ReconfigurationCaseEventHandler eventInformation:{}", reconfigurationEventInformation);
        results.stream()
            .filter(CancellationEvaluateResponse.class::isInstance)
            .map(CancellationEvaluateResponse.class::cast)
            .filter(result ->
                CancellationActions.RECONFIGURE == CancellationActions.from(result.getAction().getValue())
            )
            .forEach(reconfigureResponse -> {
                log.info("sendReconfigurationRequest request:{}", reconfigureResponse);
                evaluateReconfigureActionResponse(reconfigureResponse);
                sendReconfigurationRequest(reconfigurationEventInformation.getCaseId());
            });
    }

    private void evaluateReconfigureActionResponse(CancellationEvaluateResponse response) {
        if (response.getWarningCode() != null
                && isNotBlank(response.getWarningCode().getValue())
                || response.getWarningText() != null
                   && isNotBlank(response.getWarningText().getValue())
                || response.getProcessCategories() != null
                   && isNotBlank(response.getProcessCategories().getValue())) {
            log.warn(
                "DMN configuration has provided fields not suitable for Reconfiguration and they will be ignored"
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

    private void sendReconfigurationRequest(String caseReference) {
        TaskOperationRequest taskOperationRequest = buildTaskOperationRequest(caseReference);
        taskManagementApiClient.performOperation(serviceAuthGenerator.generate(), taskOperationRequest);
        log.info("Reconfiguration completed caseReference:{}", caseReference);
    }

    private TaskOperationRequest buildTaskOperationRequest(String caseReference) {
        TaskOperation operation = new TaskOperation(
            TaskOperationName.MARK_TO_RECONFIGURE, UUID.randomUUID().toString(),120
        );
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseReference), TaskFilterOperator.IN
        );
        return new TaskOperationRequest(operation, List.of(filter));
    }

}

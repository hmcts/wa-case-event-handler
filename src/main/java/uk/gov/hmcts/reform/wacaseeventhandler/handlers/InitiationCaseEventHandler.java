package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_INITIATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition.APPEAL_TYPE;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition.DATE_DUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition.LAST_MODIFIED_DIRECTION;

@Service
@Order(3)
@Slf4j
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UseConcurrentHashMap", "unchecked"})
public class InitiationCaseEventHandler implements CaseEventHandler {

    private final AuthTokenGenerator serviceAuthGenerator;
    private final WorkflowApiClient workflowApiClient;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;
    private final IsoDateFormatter isoDateFormatter;
    private final DueDateService dueDateService;

    @Autowired
    public InitiationCaseEventHandler(AuthTokenGenerator serviceAuthGenerator,
                                      WorkflowApiClient workflowApiClient,
                                      IdempotencyKeyGenerator idempotencyKeyGenerator,
                                      IsoDateFormatter isoDateFormatter,
                                      DueDateService dueDateService
    ) {
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.workflowApiClient = workflowApiClient;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
        this.isoDateFormatter = isoDateFormatter;
        this.dueDateService = dueDateService;
    }

    @Override
    public List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_INITIATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );

        String tenantId = eventInformation.getJurisdictionId().toLowerCase(Locale.ENGLISH);
        String directionDueDate = extractDirectionDueDate(eventInformation.getAdditionalData());
        log.debug("Direction Due Date : {}", directionDueDate);

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest(
            eventInformation.getEventId(),
            eventInformation.getNewStateId(),
            readValue(eventInformation.getAdditionalData(), APPEAL_TYPE),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            directionDueDate
        );

        EvaluateDmnResponse<InitiateEvaluateResponse> response = workflowApiClient.evaluateInitiationDmn(
            serviceAuthGenerator.generate(),
            tableKey,
            tenantId,
            evaluateDmnRequest);

        return response.getResults();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        results.stream()
            .filter(InitiateEvaluateResponse.class::isInstance)
            .map(InitiateEvaluateResponse.class::cast)
            .forEach(initiateEvaluateResponse -> {
                SendMessageRequest request =
                    buildInitiateTaskMessageRequest(initiateEvaluateResponse, eventInformation);

                workflowApiClient.sendMessage(
                    serviceAuthGenerator.generate(),
                    request
                );
            });
    }

    private String readValue(AdditionalData additionalData, CaseEventFieldsDefinition caseField) {
        if (additionalData != null && additionalData.getData() != null) {
            return (String) ofNullable(additionalData.getData()).orElse(emptyMap())
                .get(caseField.value());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractDirectionDueDate(AdditionalData additionalData) {
        if (additionalData != null) {
            Map<String, Object> data = additionalData.getData();
            if (data != null) {
                return ofNullable((Map<String, String>) data.get(LAST_MODIFIED_DIRECTION.value())).orElse(emptyMap())
                    .get(DATE_DUE.value());
            }
        }
        return null;
    }

    private EvaluateDmnRequest buildEvaluateDmnRequest(
        String eventId,
        String newStateId,
        String appealType,
        String now,
        String directionDueDate

    ) {

        Map<String, DmnValue<?>> variables = Map.of(
            "eventId", dmnStringValue(eventId),
            "postEventState", dmnStringValue(newStateId),
            "appealType", dmnStringValue(appealType),
            "now", dmnStringValue(now),
            "directionDueDate", dmnStringValue(directionDueDate)
        );

        return new EvaluateDmnRequest(variables);
    }

    private SendMessageRequest buildInitiateTaskMessageRequest(
        InitiateEvaluateResponse initiateEvaluateResponse,
        EventInformation eventInformation
    ) {
        Map<String, DmnValue<?>> initialProcessVariables = buildProcessVariables(
            initiateEvaluateResponse,
            eventInformation
        );

        log.debug("Initiation send message process variables {}", initialProcessVariables);

        return SendMessageRequest.builder()
            .messageName(TASK_INITIATION.getMessageName())
            .processVariables(initialProcessVariables)
            .build();
    }

    private Map<String, DmnValue<?>> buildProcessVariables(
        InitiateEvaluateResponse initiateEvaluateResponse,
        EventInformation eventInformation
    ) {
        final ZonedDateTime zonedDateTime = isoDateFormatter.formatToZone(eventInformation.getEventTimeStamp());

        ZonedDateTime delayUntil = dueDateService.calculateDelayUntil(
            zonedDateTime,
            cannotBeNull(initiateEvaluateResponse.getDelayDuration()).getValue()
        );

        ZonedDateTime dueDate = dueDateService.calculateDueDate(
            delayUntil,
            cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()).getValue()
        );

        String idempotencyKey = idempotencyKeyGenerator.generateIdempotencyKey(
            eventInformation.getEventInstanceId(),
            initiateEvaluateResponse.getTaskId().getValue()
        );


        Map<String, DmnValue<?>> processVariables = new HashMap<>();

        // Required process variables
        processVariables.put("idempotencyKey", dmnStringValue(idempotencyKey));
        processVariables.put("taskState", dmnStringValue("unconfigured"));
        processVariables.put("caseTypeId", dmnStringValue(eventInformation.getCaseTypeId()));
        processVariables.put("dueDate", dmnStringValue(dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        processVariables.put("workingDaysAllowed", cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()));
        processVariables.put("group", initiateEvaluateResponse.getGroup());
        processVariables.put("jurisdiction", dmnStringValue(eventInformation.getJurisdictionId()));
        processVariables.put("name", initiateEvaluateResponse.getName());
        processVariables.put("taskId", initiateEvaluateResponse.getTaskId());
        processVariables.put("caseId", dmnStringValue(eventInformation.getCaseId()));
        processVariables.put("delayUntil", dmnStringValue(delayUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        processVariables.put("hasWarnings", dmnBooleanValue(false));

        // Optional process variables this would be deprecated
        if (initiateEvaluateResponse.getTaskCategory() != null) {
            processVariables.put("taskCategory", initiateEvaluateResponse.getTaskCategory());
        }

        // If it contains process categories and set to true (new format) add to processVariables map.
        if (!initiateEvaluateResponse.getProcessCategories().isEmpty()) {
            initiateEvaluateResponse.getProcessCategories().forEach((key, value) -> {
                if (value.getValue()) {
                    processVariables.put(key, value);
                }
            });
        }


        return processVariables;
    }

    private DmnValue<Integer> cannotBeNull(DmnValue<Integer> workingDaysAllowed) {
        return workingDaysAllowed == null ? dmnIntegerValue(0) : workingDaysAllowed;
    }

}

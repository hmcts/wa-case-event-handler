package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilConfigurator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnAndMessageNames.TASK_INITIATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnMapValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition.DATE_DUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.ia.CaseEventFieldsDefinition.LAST_MODIFIED_DIRECTION;

@Service
@Order(4)
@Slf4j
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.ExcessiveImports",
    "PMD.ReturnEmptyCollectionRatherThanNull", "unchecked"})
public class InitiationCaseEventHandler implements CaseEventHandler {

    private final AuthTokenGenerator serviceAuthGenerator;
    private final WorkflowApiClient workflowApiClient;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;
    private final IsoDateFormatter isoDateFormatter;
    private final DueDateService dueDateService;
    private final ObjectMapper objectMapper;
    private final DelayUntilConfigurator delayUntilConfigurator;

    @Autowired
    public InitiationCaseEventHandler(AuthTokenGenerator serviceAuthGenerator,
                                      WorkflowApiClient workflowApiClient,
                                      IdempotencyKeyGenerator idempotencyKeyGenerator,
                                      IsoDateFormatter isoDateFormatter,
                                      DueDateService dueDateService,
                                      ObjectMapper objectMapper,
                                      DelayUntilConfigurator delayUntilConfigurator) {
        this.serviceAuthGenerator = serviceAuthGenerator;
        this.workflowApiClient = workflowApiClient;
        this.idempotencyKeyGenerator = idempotencyKeyGenerator;
        this.isoDateFormatter = isoDateFormatter;
        this.dueDateService = dueDateService;
        this.objectMapper = objectMapper;
        this.delayUntilConfigurator = delayUntilConfigurator;
    }

    @Override
    public List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        String tableKey = TASK_INITIATION.getTableKey(
            eventInformation.getJurisdictionId(),
            eventInformation.getCaseTypeId()
        );
        log.debug("tableKey : {}", tableKey);
        String tenantId = eventInformation.getJurisdictionId();
        String directionDueDate = extractDirectionDueDate(eventInformation.getAdditionalData());
        log.debug("Direction Due Date : {}", directionDueDate);

        EvaluateDmnRequest evaluateDmnRequest = buildEvaluateDmnRequest(
            eventInformation.getEventId(),
            eventInformation.getNewStateId(),
            readValue(eventInformation.getAdditionalData()),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            directionDueDate
        );
        log.info("EvaluateDmnRequest : {}", evaluateDmnRequest);
        EvaluateDmnResponse<InitiateEvaluateResponse> response = workflowApiClient.evaluateInitiationDmn(
            serviceAuthGenerator.generate(),
            tableKey,
            tenantId,
            evaluateDmnRequest
        );
        log.debug("Workflow api response : {}", response);
        return response.getResults();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        log.info("InitiationCaseEventHandler eventInformation:{}", eventInformation);
        results.stream()
            .filter(InitiateEvaluateResponse.class::isInstance)
            .map(InitiateEvaluateResponse.class::cast)
            .forEach(initiateEvaluateResponse -> {
                log.info("initiateEvaluateResponse is {}", initiateEvaluateResponse);
                SendMessageRequest request =
                    buildInitiateTaskMessageRequest(initiateEvaluateResponse, eventInformation);

                log.info("sendInitiationMessage message:{}", request);
                workflowApiClient.sendMessage(
                    serviceAuthGenerator.generate(),
                    request
                );
            });
    }

    private Map<String, Object> readValue(AdditionalData additionalData) {
        if (additionalData != null) {
            return objectMapper.convertValue(additionalData, Map.class);
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
        Map<String, Object> additionalData,
        String now,
        String directionDueDate

    ) {

        Map<String, DmnValue<?>> variables = Map.of(
            "eventId", dmnStringValue(eventId),
            "postEventState", dmnStringValue(newStateId),
            "additionalData", dmnMapValue(additionalData),
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
            .tenantId("wa")
            .build();
    }

    private Map<String, DmnValue<?>> buildProcessVariables(
        InitiateEvaluateResponse initiateEvaluateResponse,
        EventInformation eventInformation
    ) {
        final ZonedDateTime zonedDateTime = isoDateFormatter.formatToZone(eventInformation.getEventTimeStamp());

        ZonedDateTime delayUntilBasedOnDelayDuration = dueDateService.calculateDelayUntil(
            zonedDateTime,
            cannotBeNull(initiateEvaluateResponse.getDelayDuration()).getValue()
        );

        ZonedDateTime delayUntil = ofNullable(initiateEvaluateResponse.getDelayUntil())
            .map(input -> {
                ZoneId systemDefault = ZoneId.systemDefault();
                log.info("System default zone : {}", systemDefault);
                DelayUntilRequest delayUntilRequest = input.getValue();
                LocalDateTime calculateDelayUntil = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
                log.info("Calculated DelayUntil date is: {}", calculateDelayUntil);
                ZonedDateTime dateTimeOnDefaultZone = calculateDelayUntil.atZone(systemDefault);
                log.info("Calculated DelayUntil on DefaultZone is: {}", dateTimeOnDefaultZone);
                return dateTimeOnDefaultZone;
            })
            .orElse(delayUntilBasedOnDelayDuration);

        ZonedDateTime dueDate = dueDateService.calculateDueDate(
            delayUntil,
            cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()).getValue()
        );

        String idempotencyKey = idempotencyKeyGenerator.generateIdempotencyKey(
            eventInformation.getEventInstanceId(),
            initiateEvaluateResponse.getTaskId().getValue()
        );


        Map<String, DmnValue<?>> processVariables = new ConcurrentHashMap<>();

        // Required process variables
        processVariables.put("idempotencyKey", dmnStringValue(idempotencyKey));
        processVariables.put("taskState", dmnStringValue("unconfigured"));
        processVariables.put("caseTypeId", dmnStringValue(eventInformation.getCaseTypeId()));
        processVariables.put("dueDate", dmnStringValue(dueDate.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("workingDaysAllowed", cannotBeNull(initiateEvaluateResponse.getWorkingDaysAllowed()));
        processVariables.put("jurisdiction", dmnStringValue(eventInformation.getJurisdictionId()));
        processVariables.put("name", initiateEvaluateResponse.getName());
        processVariables.put("taskId", initiateEvaluateResponse.getTaskId());
        processVariables.put("caseId", dmnStringValue(eventInformation.getCaseId()));
        processVariables.put("delayUntil", dmnStringValue(delayUntil.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("hasWarnings", dmnBooleanValue(false));
        processVariables.put("warningList", dmnStringValue(new WarningValues().getValuesAsJson()));

        // Optional process variables this would be deprecated
        if (initiateEvaluateResponse.getTaskCategory() != null) {
            processVariables.put("taskCategory", initiateEvaluateResponse.getTaskCategory());
        }

        // If it contains process categories and set to true (new format) add to processVariables map.
        if (initiateEvaluateResponse.getProcessCategories() != null
            && initiateEvaluateResponse.getProcessCategories().getValue() != null) {

            String categories = initiateEvaluateResponse.getProcessCategories().getValue();

            List<String> categoriesToAdd = Stream.of(categories.split(","))
                .map(String::trim).toList();

            categoriesToAdd.forEach(cat ->
                                        processVariables.put("__processCategory__" + cat, dmnBooleanValue(true))
            );
        }


        return processVariables;
    }

    private DmnValue<Integer> cannotBeNull(DmnValue<Integer> workingDaysAllowed) {
        return workingDaysAllowed == null ? dmnIntegerValue(0) : workingDaysAllowed;
    }
}

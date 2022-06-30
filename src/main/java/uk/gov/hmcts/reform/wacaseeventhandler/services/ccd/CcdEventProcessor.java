package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.TASK_INITIATION_FEATURE;

@Slf4j
@Service
public class CcdEventProcessor {

    private final List<CaseEventHandler> handlerServices;
    private final ObjectMapper objectMapper;
    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;


    public CcdEventProcessor(List<CaseEventHandler> handlerServices,
                             ObjectMapper objectMapper,
                             LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.handlerServices = handlerServices;
        this.objectMapper = objectMapper;
        this.featureFlagProvider = featureFlagProvider;
    }


    public void processMessage(String message) throws JsonProcessingException {
        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
        processMessage(eventInformation);
    }

    public void processMessage(CaseEventMessage caseEventMessage) throws JsonProcessingException {
        EventInformation eventInformation =
            objectMapper.readValue(caseEventMessage.getMessageContent(), EventInformation.class);
        processMessage(eventInformation);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void processMessage(EventInformation eventInformation) {
        LinkedHashMap<String, String> message = new LinkedHashMap<>();
        message.put("Case details", null);
        message.put("Case id", eventInformation.getCaseId());
        message.put("Event id", eventInformation.getEventId());
        message.put("New state id", eventInformation.getNewStateId());
        message.put("Previous state id", eventInformation.getPreviousStateId());
        message.put("Jurisdiction id", eventInformation.getJurisdictionId());
        message.put("Case type id", eventInformation.getCaseTypeId());
        message.put("Event instance id", eventInformation.getEventInstanceId());
        message.put("Timestamp", eventInformation.getEventTimeStamp().toString());
        message.put("User id", eventInformation.getUserId());

        if (eventInformation.getAdditionalData() != null && eventInformation.getAdditionalData().getData() != null) {
            message.put("Additional data", null);

            for (Map.Entry<String, Object> pair : eventInformation.getAdditionalData().getData().entrySet()) {
                message.put(pair.getKey(), "*sensitive information*");
            }
        }

        StringBuilder logInfo = new StringBuilder();
        for (Map.Entry<String, String> pair : message.entrySet()) {
            if (pair.getValue() == null) {
                logInfo.append(pair.getKey()).append(": \n");
            } else {
                logInfo.append(pair.getKey()).append(": '").append(pair.getValue()).append("'\n");
            }
        }

        log.info(logInfo.toString());

        boolean isTaskInitiationEnabled =
                featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE, eventInformation.getUserId()
        );

        if (isTaskInitiationEnabled) {
            handlerServices.forEach(handler -> {
                List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
                if (results.isEmpty()) {
                    log.info("No results returned when evaluating {}", handler.getClass().getName());
                } else {
                    handler.handle(results, eventInformation);
                }
            });
        } else {
            log.info("Feature flag '{}' evaluated to false. Message consumed but not being processed",
                     TASK_INITIATION_FEATURE
            );
        }
    }

}

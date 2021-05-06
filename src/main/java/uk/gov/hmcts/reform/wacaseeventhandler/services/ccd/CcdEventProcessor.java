package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.List;

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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void processMessage(String message) throws JsonProcessingException {
        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);

        log.info("Message details: {}", eventInformation);

        boolean isTaskInitiationEnabled = featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE,
                                                                              eventInformation.getUserId());

        if (isTaskInitiationEnabled) {
            handlerServices.forEach(handler -> {
                List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
                if (!results.isEmpty()) {
                    handler.handle(results, eventInformation);
                }
            });
        } else {
            log.info(
                "Feature flag '{}' evaluated to false. Message consumed but not being processed",
                TASK_INITIATION_FEATURE
            );

        }
    }

}

package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CaseEventHandlerLivenessHealthController extends LivenessStateHealthIndicator {

    @Value("${management.endpoint.health.newMessageStateThresholdForLivenessCheck}")
    private int newMessageStateThresholdForLivenessCheck;

    @Value("${management.endpoint.health.newMessageLivenessStateCheckEnvEnabled}")
    private String newMessageLivenessStateCheckEnvEnabled;

    @Value("${environment}")
    private String environment;

    CaseEventMessageService caseEventMessageService;

    @Autowired
    public CaseEventHandlerLivenessHealthController(ApplicationAvailability availability,
                                                    CaseEventMessageService caseEventMessageService) {
        super(availability);
        this.caseEventMessageService = caseEventMessageService;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        log.info("CaseEventHandler Liveness check Invoked for environment {} ", environment);
        log.info("CaseEventHandler Liveness check configured for environments {} ",
                 newMessageLivenessStateCheckEnvEnabled);

        if (StringUtils.isNoneBlank(environment) && isEnabledForEnvironment(environment)) {
            final List<CaseEventMessageEntity> allMessageInNewState =
                caseEventMessageService.getAllMessagesInNewState(environment);
            final int minNoOfMessages = 1;
            final long numberOfHours = 1L;
            final int noOfNewMessages = allMessageInNewState.size();

            if (noOfNewMessages > minNoOfMessages
                && noOfNewMessages > newMessageStateThresholdForLivenessCheck
                && allMessageInNewState.get(noOfNewMessages - 1).getEventTimestamp()
                    .isBefore(LocalDateTime.now().minusHours(numberOfHours))) {
                return LivenessState.BROKEN;
            }
        }
        return LivenessState.CORRECT;
    }


    private boolean isEnabledForEnvironment(String env) {
        Set<String> envsToEnable = Arrays.stream(newMessageLivenessStateCheckEnvEnabled.split(","))
            .map(String::trim).collect(Collectors.toSet());
        return envsToEnable.contains(env);
    }

}

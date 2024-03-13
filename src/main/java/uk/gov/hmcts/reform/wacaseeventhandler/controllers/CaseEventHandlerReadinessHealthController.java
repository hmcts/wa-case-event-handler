package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageCacheService;

@Slf4j
@Component
public class CaseEventHandlerReadinessHealthController extends ReadinessStateHealthIndicator  {

    CaseEventMessageCacheService caseEventMessageCacheService;

    @Value("${environment}")
    private String environment;

    @Autowired
    public CaseEventHandlerReadinessHealthController(ApplicationAvailability availability,
                                                     CaseEventMessageCacheService caseEventMessageCacheService) {
        super(availability);
        this.caseEventMessageCacheService = caseEventMessageCacheService;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        log.info("CaseEventHandler Readiness check Invoked");
        try {
            caseEventMessageCacheService.getAllMessagesInNewState(environment);
        } catch (Exception e) {
            log.error("Readiness check failed with exception {} ", e.getMessage());
            return ReadinessState.REFUSING_TRAFFIC;
        }
        return ReadinessState.ACCEPTING_TRAFFIC;
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

@Slf4j
@Component
public class CaseEventHandlerReadinessHealthController extends ReadinessStateHealthIndicator  {

    CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    public CaseEventHandlerReadinessHealthController(ApplicationAvailability availability,
                                                     CaseEventMessageRepository caseEventMessageRepository) {
        super(availability);
        this.caseEventMessageRepository = caseEventMessageRepository;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        log.info("CaseEventHandler Readiness check Invoked");
        try {
            caseEventMessageRepository.getAllMessagesInNewState();
        } catch (Exception e) {
            return ReadinessState.REFUSING_TRAFFIC;
        }
        return ReadinessState.ACCEPTING_TRAFFIC;
    }
}

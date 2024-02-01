package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;

@Slf4j
@Component
public class CaseEventHandlerLivenessHealthController extends LivenessStateHealthIndicator {

    CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    public CaseEventHandlerLivenessHealthController(ApplicationAvailability availability,
                                                    CaseEventMessageRepository caseEventMessageRepository) {
        super(availability);
        this.caseEventMessageRepository = caseEventMessageRepository;
    }

    @Override
    protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
        log.info("CaseEventHandler Liveness check Invoked");
        final int maxNoOfMessagesInNewState = 100;

        final List<CaseEventMessageEntity> allMessageInNewState =
            caseEventMessageRepository.getAllMessagesInNewState();

        if (allMessageInNewState.size() > maxNoOfMessagesInNewState) {
            return LivenessState.BROKEN;
        } else {
            return LivenessState.CORRECT;
        }

    }

}

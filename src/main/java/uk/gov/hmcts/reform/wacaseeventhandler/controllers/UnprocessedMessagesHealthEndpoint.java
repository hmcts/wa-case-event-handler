package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

@Component("ccdMessagesInNewState")
public class UnprocessedMessagesHealthEndpoint implements HealthIndicator {

    public static final String RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE = "%s messages in NEW state";

    private static final String CASE_EVENT_HANDLER_MESSAGE_STATE_HEALTH = "caseEventHandlerMessageStateHealth";

    @Value("${management.endpoint.health.newMessageStateThreshold}")
    private int newMessageStateThreshold;

    @Autowired
    private CaseEventMessageRepository repository;

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        int numberOfMessagesInNewState = repository.getNumberOfMessagesInNewState();

        Health.Builder healthBuilder =
            numberOfMessagesInNewState > newMessageStateThreshold ? Health.down() : Health.up();

        return healthBuilder
                .withDetail(CASE_EVENT_HANDLER_MESSAGE_STATE_HEALTH,
                            String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, numberOfMessagesInNewState))
                .build();
    }
}

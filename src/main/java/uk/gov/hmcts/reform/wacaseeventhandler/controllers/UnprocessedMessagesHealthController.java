package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component("ccdMessagesInNewState")
public class UnprocessedMessagesHealthController implements HealthIndicator {

    public static final String RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE = "%s messages in NEW state";
    public static final String CHECK_DISABLED_MESSAGE = "check disabled in %s";

    private static final String CASE_EVENT_HANDLER_MESSAGE_STATE_HEALTH = "caseEventHandlerMessageStateHealth";

    @Value("${management.endpoint.health.newMessageStateThreshold}")
    private int newMessageStateThreshold;

    @Value("${management.endpoint.health.newMessageStateCheckEnvEnabled}")
    private String newMessageStateCheckEnvEnabled;

    @Value("${environment}")
    private String environment;

    private final CaseEventMessageRepository repository;

    @Autowired
    public UnprocessedMessagesHealthController(CaseEventMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        if (isEnabledForEnvironment(environment)) {
            int numberOfMessagesInNewState = repository.getNumberOfMessagesInNewState();

            Health.Builder healthBuilder =
                numberOfMessagesInNewState > newMessageStateThreshold ? Health.down() : Health.up();

            return healthBuilder
                .withDetail(
                    CASE_EVENT_HANDLER_MESSAGE_STATE_HEALTH,
                    String.format(RETRIEVED_NUMBER_OF_MESSAGES_IN_NEW_STATE, numberOfMessagesInNewState)
                )
                .build();
        } else {
            return Health.up().withDetail(
                CASE_EVENT_HANDLER_MESSAGE_STATE_HEALTH,
                String.format(CHECK_DISABLED_MESSAGE, environment)
            ).build();
        }
    }

    private boolean isEnabledForEnvironment(String env) {
        Set<String> envsToEnable = Arrays.stream(newMessageStateCheckEnvEnabled.split(","))
            .map(String::trim).collect(Collectors.toSet());
        return envsToEnable.contains(env);
    }
}

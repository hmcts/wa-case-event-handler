package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component("ccdMessagesReceived")
@Slf4j
public class ReceivedMessagesHealthController implements HealthIndicator {

    protected static final String CASE_EVENT_HANDLER_MESSAGE_HEALTH = "caseEventHandlerMessageHealth";
    protected static final String NO_MESSAGES_RECEIVED = "No messages received from CCD during the past hour";
    protected static final String MESSAGES_RECEIVED = "Messages received from CCD during the past hour";

    protected static final String NO_MESSAGE_CHECK = "Out Of Hours, no check for messages";
    protected static final String CHECK_DISABLED_MESSAGE = "check disabled in %s";

    protected static final String ENV_AAT = "aat";

    protected static final String STAGING_TEXT = "staging";

    @Value("${management.endpoint.health.receivedMessageCheckEnvEnabled}")
    private String receivedMessageCheckEnvEnabled;

    @Value("${environment}")
    private String environment;

    @Autowired
    private CaseEventMessageRepository repository;

    @Autowired
    private Clock clock;

    @Autowired
    private HolidayService holidayService;

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {

        if (isNotEnabledForEnvironment(environment)) {
            return Health
                .up()
                .withDetail(CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                            String.format(CHECK_DISABLED_MESSAGE, environment)
                )
                .build();
        }

        ZoneId ukTimeZone = ZoneId.of("Europe/London");
        ZonedDateTime utcDateTime = LocalDateTime.now(clock).atZone(ZoneOffset.UTC);
        ZonedDateTime ukZonedDateTime = utcDateTime.withZoneSameInstant(ukTimeZone);
        LocalDateTime ukLocalDateTime = ukZonedDateTime.toLocalDateTime();

        log.info("UTC date and time {}, UK local date and time {}", ukZonedDateTime, ukLocalDateTime);

        if (isDateWithinWorkingHours(ukLocalDateTime)) {
            if (repository.getNumberOfMessagesReceivedInLastHour(ukLocalDateTime) == 0) {
                return Health
                    .down()
                    .withDetail(
                        CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                        NO_MESSAGES_RECEIVED
                    )
                    .build();
            } else {
                return Health
                    .up()
                    .withDetail(CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                                MESSAGES_RECEIVED
                    )
                    .build();
            }
        } else {
            return Health
                .up()
                .withDetail(CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                            NO_MESSAGE_CHECK)
                .build();
        }
    }

    private boolean isDateWithinWorkingHours(LocalDateTime localDateTime) {
        if (holidayService.isWeekend(localDateTime.toLocalDate())
            || holidayService.isHoliday(localDateTime.toLocalDate())) {
            return false;
        }

        LocalDateTime workingHoursStartTime = LocalDateTime.of(localDateTime.getYear(),
                                                               localDateTime.getMonth(),
                                                               localDateTime.getDayOfMonth(),
                                                               8,30);
        LocalDateTime workingHoursEndTime = LocalDateTime.of(localDateTime.getYear(),
                                                             localDateTime.getMonth(),
                                                             localDateTime.getDayOfMonth(),
                                                             17,30);

        return (localDateTime.equals(workingHoursStartTime) || localDateTime.isAfter(workingHoursStartTime))
            && (localDateTime.equals(workingHoursEndTime) || localDateTime.isBefore(workingHoursEndTime));
    }

    public boolean isNotEnabledForEnvironment(String env) {
        if (ENV_AAT.equals(env)) {

            URI currentUri = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
            String currentUriString = "";
            if (null != currentUri) {
                currentUriString = currentUri.toString();
            }
            if (currentUriString.contains(STAGING_TEXT)) {
                return true;
            }
        }
        Set<String> envsToEnable = Arrays.stream(receivedMessageCheckEnvEnabled.split(","))
            .map(String::trim).collect(Collectors.toSet());
        return !envsToEnable.contains(env);
    }
}

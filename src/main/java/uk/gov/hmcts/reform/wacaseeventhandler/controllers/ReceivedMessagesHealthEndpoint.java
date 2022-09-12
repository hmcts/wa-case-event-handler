package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.Clock;
import java.time.LocalDateTime;

@Component("ccdMessagesReceived")
public class ReceivedMessagesHealthEndpoint implements HealthIndicator {

    private  static final String CASE_EVENT_HANDLER_MESSAGE_HEALTH = "Case Event Handler Message Health";

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

        LocalDateTime now = LocalDateTime.now(clock).minusHours(1);

        if (isDateWithinWorkingHours(now)) {
            if (repository.getNumberOfMessagesReceivedInLastHour(now) == 0) {
                return Health
                    .down()
                    .withDetail(
                        CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                        "No messages received from CCD during the past hour")
                    .build();
            } else {
                return Health
                    .up()
                    .withDetail(CASE_EVENT_HANDLER_MESSAGE_HEALTH,
                                "Messages received from CCD during the past hour")
                    .build();
            }
        } else {
            return Health.up().build();
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
}

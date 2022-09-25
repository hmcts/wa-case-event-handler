package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
public class ReceivedMessagesHealthEndpoint {

    protected static final String CASE_EVENT_HANDLER_MESSAGE_HEALTH = "caseEventHandlerMessageHealth";
    protected static final String NO_MESSAGES_RECEIVED = "No messages received from CCD during the past hour";
    protected static final String MESSAGES_RECEIVED = "Messages received from CCD during the past hour";

    protected static final String NO_MESSAGE_CHECK = "Out Of Hours, no check for messages";

    @Autowired
    private CaseEventMessageRepository repository;

    @Autowired
    private Clock clock;

    @Autowired
    private HolidayService holidayService;

    @Operation(description = "Query case event message db to find problematic messages by job name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Health.class))})
    })
    @GetMapping(path = "/ccdMessagesReceived/health")
    @ResponseStatus(HttpStatus.OK)
    public Health health() {

        LocalDateTime now = LocalDateTime.now(clock).minusHours(1);

        if (isDateWithinWorkingHours(now)) {
            if (repository.getNumberOfMessagesReceivedInLastHour(now) == 0) {
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
}

package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.Builder;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.buildInitiateTaskDmnRequest;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.validAdditionalData;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withEmptyDirectionDueDate;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withoutDirectionDueDate;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withoutLastModifiedDirection;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    private static final String DMN_NAME = "wa-task-initiation-ia-asylum";
    public static final String TENANT_ID = "ia";
    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;
    @Mock
    private IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Captor
    private ArgumentCaptor<SendMessageRequest<InitiateProcessVariables, CorrelationKeys>> captor;

    @Mock
    private IsoDateFormatter isoDateFormatter;

    @Mock
    private DueDateService dueDateService;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    private final String eventInstanceId = UUID.randomUUID().toString();

    @ParameterizedTest
    @MethodSource("provideEventInformation")
    void evaluateDmn(EventInformation eventInformation, String directionDueDate) {
        EvaluateDmnRequest<InitiateEvaluateRequest> requestParameters =
            buildInitiateTaskDmnRequest(directionDueDate);

        Mockito.when(apiClientToInitiateTask.evaluateDmn(
            DMN_NAME,
            requestParameters,
            TENANT_ID
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        Mockito.verify(apiClientToInitiateTask).evaluateDmn(
            eq(DMN_NAME),
            eq(requestParameters),
            eq(TENANT_ID)
        );
    }

    @ParameterizedTest
    @MethodSource("dateTimeScenario")
    void handle(HandleDateTimeScenario handleDateTimeScenario) {
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            LocalDateTime.parse(handleDateTimeScenario.inputDate), ZoneId.of("Europe/London")
        );

        Mockito.when(isoDateFormatter.formatToZone(LocalDateTime.parse(handleDateTimeScenario.inputDate)))
            .thenReturn(zonedDateTime);
        final LocalTime fourPmTime = LocalTime.of(16, 0, 0, 0);
        final ZonedDateTime zonedDateTimeAt4Pm = ZonedDateTime.of(
            zonedDateTime.toLocalDate(), fourPmTime, ZoneId.of("Europe/London")
        );

        InitiateEvaluateResponse initiateTaskResponse1 = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .delayDuration(new DmnIntegerValue(0))
            .workingDaysAllowed(new DmnIntegerValue(0))
            .taskCategory(new DmnStringValue("Case progression"))
            .build();

        // response without delayDuration and WorkingDaysAllowed
        InitiateEvaluateResponse initiateTaskResponse2 = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("external"))
            .name(new DmnStringValue("Decide On Time Extension"))
            .taskId(new DmnStringValue("decideOnTimeExtension"))
            .taskCategory(new DmnStringValue("Time extension"))
            .build();

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "decideOnTimeExtension"))
            .thenReturn("idempotencyKey2");

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse1, initiateTaskResponse2);

        when(dueDateService.calculateDelayUntil(zonedDateTime, 0))
            .thenReturn(zonedDateTime);

        when(dueDateService.calculateDueDate(zonedDateTime, 0))
            .thenReturn(zonedDateTimeAt4Pm);

        handlerService.handle(results, getEventInformation(eventInstanceId,
                                                           handleDateTimeScenario.inputDate));

        Mockito.verify(apiClientToInitiateTask, Mockito.times(2)).sendMessage(captor.capture());
        SendMessageRequest<InitiateProcessVariables, CorrelationKeys> actualSendMessageRequest = captor.getValue();

        assertThat(captor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "Case progression",
            "TCW",
            handleDateTimeScenario.dateAt4pm,
            handleDateTimeScenario.expectedDate,
            0
        ));

        assertThat(captor.getAllValues().get(1)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey2",
            "Decide On Time Extension",
            "decideOnTimeExtension",
            "Time extension",
            "external",
            handleDateTimeScenario.dateAt4pm,
            handleDateTimeScenario.expectedDate,
            0
        ));
    }

    @Test
    void handle_when_delay_duration_is_not_zero() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            LocalDateTime.parse("2020-12-08T15:53:36.530377"), ZoneId.of("Europe/London")
        );

        Mockito.when(isoDateFormatter.formatToZone(LocalDateTime.parse("2020-12-08T15:53:36.530377")))
            .thenReturn(zonedDateTime);

        final ZonedDateTime expectedDelayUntil = ZonedDateTime.of(
            LocalDateTime.parse("2020-12-10T16:00:00"), ZoneId.of("Europe/London")
        );

        final ZonedDateTime expectedDueDate = ZonedDateTime.of(
            LocalDateTime.parse("2020-12-12T16:00:00"), ZoneId.of("Europe/London")
        );

        InitiateEvaluateResponse initiateTaskResponse1 = InitiateEvaluateResponse.builder()
            .group(new DmnStringValue("TCW"))
            .name(new DmnStringValue("Process Application"))
            .taskId(new DmnStringValue("processApplication"))
            .delayDuration(new DmnIntegerValue(2))
            .workingDaysAllowed(new DmnIntegerValue(2))
            .taskCategory(new DmnStringValue("Case progression"))
            .build();

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse1);

        when(dueDateService.calculateDelayUntil(zonedDateTime, 2))
            .thenReturn(expectedDelayUntil);

        when(dueDateService.calculateDueDate(expectedDelayUntil, 2))
            .thenReturn(expectedDueDate);

        handlerService.handle(results, getEventInformation(eventInstanceId, "2020-12-08T15:53:36.530377"));

        Mockito.verify(apiClientToInitiateTask, Mockito.times(1)).sendMessage(captor.capture());

        assertThat(captor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "Case progression",
            "TCW",
            "2020-12-12T16:00:00",
            "2020-12-10T16:00:00",
            2
        ));
    }

    @Test
    void handleWhenInitiationResponseIsEmpty() {
        List<InitiateEvaluateResponse> results = Lists.emptyList();

        handlerService.handle(results, getEventInformation(eventInstanceId, "2020-03-29T10:53:36.530377"));

        verify(apiClientToInitiateTask, times(0)).sendMessage(any());
    }

    private static EventInformation getEventInformation(
        String eventInstanceId, String eventTimeStamp) {
        return EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .eventId("submitAppeal")
            .newStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.parse(eventTimeStamp))
            .build();
    }

    private static Stream<Arguments> provideEventInformation() {
        return Stream.of(
            Arguments.of(getEventInformation("eventInstanceId",
                                             "2020-03-29T10:53:36.530377"), null),
            Arguments.of(validAdditionalData(), "2021-04-06"),
            Arguments.of(withEmptyDirectionDueDate(), ""),
            Arguments.of(withoutDirectionDueDate(), null),
            Arguments.of(withoutLastModifiedDirection(), null)
        );
    }

    private SendMessageRequest<InitiateProcessVariables, CorrelationKeys> getExpectedSendMessageRequest(
        String idempotencyKey,
        String name,
        String taskId,
        String taskCategory,
        String group,
        String dueDate,
        String delayUntil,
        int workingDays
    ) {
        InitiateProcessVariables expectedInitiateTaskSendMessageRequest = InitiateProcessVariables.builder()
            .idempotencyKey(new DmnStringValue(idempotencyKey))
            .taskState(new DmnStringValue("unconfigured"))
            .caseTypeId(new DmnStringValue("asylum"))
            .jurisdiction(new DmnStringValue("ia"))
            .group(new DmnStringValue(group))
            .name(new DmnStringValue(name))
            .taskId(new DmnStringValue(taskId))
            .taskCategory(new DmnStringValue(taskCategory))
            .caseId(new DmnStringValue("some case reference"))
            .dueDate(new DmnStringValue(dueDate))
            .workingDaysAllowed(new DmnIntegerValue(workingDays))
            .delayUntil(new DmnStringValue(delayUntil))
            .build();

        return new SendMessageRequest<>(
            "createTaskMessage",
            expectedInitiateTaskSendMessageRequest,
            null, false
        );
    }

    static Stream<HandleDateTimeScenario> dateTimeScenario() {
        final HandleDateTimeScenario dateTime = HandleDateTimeScenario.builder()
            .inputDate("2020-12-08T15:53:36.530377")
            .expectedDate("2020-12-08T15:53:36.530377")
            .dateAt4pm("2020-12-08T16:00:00")
            .build();

        final HandleDateTimeScenario dstStarter = HandleDateTimeScenario.builder()
            .inputDate("2020-03-29T10:53:36.530377")
            .expectedDate("2020-03-29T10:53:36.530377")
            .dateAt4pm("2020-03-29T16:00:00")
            .build();

        final HandleDateTimeScenario dstEnd = HandleDateTimeScenario.builder()
            .inputDate("2020-10-29T10:53:36.530377")
            .expectedDate("2020-10-29T10:53:36.530377")
            .dateAt4pm("2020-10-29T16:00:00")
            .build();

        return Stream.of(dateTime, dstStarter, dstEnd);
    }

    @Builder
    static class HandleDateTimeScenario {
        String inputDate;
        String expectedDate;
        String dateAt4pm;
    }

}

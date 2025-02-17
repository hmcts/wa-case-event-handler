package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.buildInitiateTaskDmnRequest;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.validAdditionalData;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withEmptyDirectionDueDate;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withoutAppealType;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withoutDirectionDueDate;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.withoutLastModifiedDirection;

@ExtendWith(MockitoExtension.class)
class InitiationCaseEventHandlerTest {

    public static final String TENANT_ID = "ia";
    public static final String INITIATE_TASK_MESSAGE_NAME = "createTaskMessage";
    private static final String TASK_INITIATION_DMN_TABLE = "wa-task-initiation-ia-asylum";
    private static final String SERVICE_AUTH_TOKEN = "s2s token";
    @Mock
    private WorkflowApiClient workflowApiClient;
    @Mock
    private AuthTokenGenerator serviceAuthGenerator;
    @Mock
    private IdempotencyKeyGenerator idempotencyKeyGenerator;
    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;
    @Mock
    private IsoDateFormatter isoDateFormatter;
    @Mock
    private DueDateService dueDateService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InitiationCaseEventHandler handlerService;
    private String eventInstanceId;

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

    @BeforeEach
    void setUp() {
        lenient().when(serviceAuthGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        eventInstanceId = UUID.randomUUID().toString();
    }

    @ParameterizedTest
    @MethodSource("provideEventInformation")
    void evaluateDmn(EventInformation eventInformation, String directionDueDate, Map<String, Object> appealType) {
        Map<String, Object> dataMap = appealType == null || appealType.isEmpty()
            ? Collections.emptyMap() : mapAppealType();
        lenient().when(objectMapper.convertValue(eventInformation.getAdditionalData(), Map.class)).thenReturn(dataMap);
        EvaluateDmnRequest requestParameters =
            buildInitiateTaskDmnRequest(directionDueDate, appealType);

        lenient().when(workflowApiClient.evaluateInitiationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_INITIATION_DMN_TABLE,
            TENANT_ID,
            requestParameters
        )).thenReturn(new EvaluateDmnResponse<>(Collections.emptyList()));

        handlerService.evaluateDmn(eventInformation);

        verify(workflowApiClient, times(1)).evaluateInitiationDmn(
            SERVICE_AUTH_TOKEN,
            TASK_INITIATION_DMN_TABLE,
            TENANT_ID,
            requestParameters
        );
    }

    @NotNull
    private static Map<String, Object> mapAppealType() {
        Map<String, Object> appealMap = new HashMap<>();
        appealMap.put("appealType", "protection");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("data", appealMap);
        return dataMap;
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
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(0))
            .workingDaysAllowed(dmnIntegerValue(0))
            .processCategories(dmnStringValue("caseProgression"))
            .build();

        // response without delayDuration and WorkingDaysAllowed
        InitiateEvaluateResponse initiateTaskResponse2 = InitiateEvaluateResponse.builder()
            .name(dmnStringValue("Decide On Time Extension"))
            .taskId(dmnStringValue("decideOnTimeExtension"))
            .processCategories(dmnStringValue("timeExtension"))
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

        verify(workflowApiClient, Mockito.times(2))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            handleDateTimeScenario.dateAt4pm,
            handleDateTimeScenario.expectedDate,
            0
        ));

        assertThat(sendMessageRequestCaptor.getAllValues().get(1)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey2",
            "Decide On Time Extension",
            "decideOnTimeExtension",
            "__processCategory__timeExtension",
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
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .processCategories(dmnStringValue("caseProgression"))
            .build();

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse1);

        when(dueDateService.calculateDelayUntil(zonedDateTime, 2))
            .thenReturn(expectedDelayUntil);

        when(dueDateService.calculateDueDate(expectedDelayUntil, 2))
            .thenReturn(expectedDueDate);

        handlerService.handle(results, getEventInformation(eventInstanceId, "2020-12-08T15:53:36.530377"));

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            "2020-12-12T16:00:00",
            "2020-12-10T16:00:00",
            2
        ));
    }

    @Test
    void handle_when_multiple_categories() {
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
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .processCategories(dmnStringValue("caseProgression, timeExtension"))
            .build();

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        List<InitiateEvaluateResponse> results = List.of(initiateTaskResponse1);

        when(dueDateService.calculateDelayUntil(zonedDateTime, 2))
            .thenReturn(expectedDelayUntil);

        when(dueDateService.calculateDueDate(expectedDelayUntil, 2))
            .thenReturn(expectedDueDate);

        handlerService.handle(results, getEventInformation(eventInstanceId, "2020-12-08T15:53:36.530377"));

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        Map<String, DmnValue<?>> expectedProcessVariables =
            Map.ofEntries(
                entry("idempotencyKey", dmnStringValue("idempotencyKey1")),
                entry("taskState", dmnStringValue("unconfigured")),
                entry("caseTypeId", dmnStringValue("asylum")),
                entry("dueDate", dmnStringValue("2020-12-12T16:00:00")),
                entry("workingDaysAllowed", dmnIntegerValue(2)),
                entry("jurisdiction", dmnStringValue("ia")),
                entry("name", dmnStringValue("Process Application")),
                entry("taskId", dmnStringValue("processApplication")),
                entry("caseId", dmnStringValue("some case reference")),
                entry("__processCategory__caseProgression", dmnBooleanValue(true)),
                entry("__processCategory__timeExtension", dmnBooleanValue(true)),
                entry("delayUntil", dmnStringValue("2020-12-10T16:00:00")),
                entry("hasWarnings", dmnBooleanValue(false)),
                entry("warningList", dmnStringValue(new WarningValues().getValuesAsJson()))
            );

        SendMessageRequest expectation = new SendMessageRequest(
            INITIATE_TASK_MESSAGE_NAME,
            expectedProcessVariables,
            null,
            false
        );

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(expectation);
    }

    @Test
    void handleWhenInitiationResponseIsEmpty() {
        List<InitiateEvaluateResponse> results = Collections.emptyList();

        handlerService.handle(results, getEventInformation(eventInstanceId, "2020-03-29T10:53:36.530377"));

        verify(workflowApiClient, times(0)).sendMessage(eq(SERVICE_AUTH_TOKEN), any());
    }

    private SendMessageRequest getExpectedSendMessageRequest(
        String idempotencyKey,
        String name,
        String taskId,
        String processCategory,
        String dueDate,
        String delayUntil,
        int workingDays
    ) {
        Map<String, DmnValue<?>> expectedProcessVariables =
            Map.ofEntries(
                entry("idempotencyKey", dmnStringValue(idempotencyKey)),
                entry("taskState", dmnStringValue("unconfigured")),
                entry("caseTypeId", dmnStringValue("asylum")),
                entry("dueDate", dmnStringValue(dueDate)),
                entry("workingDaysAllowed", dmnIntegerValue(workingDays)),
                entry("jurisdiction", dmnStringValue("ia")),
                entry("name", dmnStringValue(name)),
                entry("taskId", dmnStringValue(taskId)),
                entry("caseId", dmnStringValue("some case reference")),
                entry(processCategory, dmnBooleanValue(true)),
                entry("delayUntil", dmnStringValue(delayUntil)),
                entry("hasWarnings", dmnBooleanValue(false)),
                entry("warningList", dmnStringValue(new WarningValues().getValuesAsJson()))
            );

        return new SendMessageRequest(
            INITIATE_TASK_MESSAGE_NAME,
            expectedProcessVariables,
            null,
            false
        );
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
        Map<String, Object> dataMap = mapAppealType();
        return Stream.of(
            Arguments.of(getEventInformation("eventInstanceId",
                "2020-03-29T10:53:36.530377"), null, null),
            Arguments.of(validAdditionalData(), "2021-04-06", dataMap),
            Arguments.of(withEmptyDirectionDueDate(), "", dataMap),
            Arguments.of(withoutDirectionDueDate(), null, dataMap),
            Arguments.of(withoutLastModifiedDirection(), null, dataMap),
            Arguments.of(withoutAppealType(), "2021-04-06", Collections.emptyMap())
        );
    }

    @Builder
    static class HandleDateTimeScenario {
        String inputDate;
        String expectedDate;
        String dateAt4pm;
    }
}

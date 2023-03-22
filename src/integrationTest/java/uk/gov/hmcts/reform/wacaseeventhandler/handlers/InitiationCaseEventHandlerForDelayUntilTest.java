package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.WarningValues;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DueDateService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilConfigurator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilObject;
import uk.gov.hmcts.reform.wacaseeventhandler.services.dates.IsoDateFormatter;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnBooleanValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"integration"})
class InitiationCaseEventHandlerForDelayUntilTest {

    private static final String SERVICE_AUTH_TOKEN = "s2s token";
    private static final String INITIATE_TASK_MESSAGE_NAME = "createTaskMessage";
    private static final String EVENT_DATE = "2022-07-19T09:00:00.000000";
    private String eventInstanceId;

    @Mock
    private AuthTokenGenerator serviceAuthGenerator;
    @Mock
    private WorkflowApiClient workflowApiClient;
    @Mock
    private IdempotencyKeyGenerator idempotencyKeyGenerator;
    @Mock
    private IsoDateFormatter isoDateFormatter;
    @Mock
    private ObjectMapper objectMapper;


    @Mock
    HolidayService holidayService;

    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;

    private DueDateService dueDateService;

    @Autowired
    private DelayUntilConfigurator delayUntilConfigurator;
    private InitiationCaseEventHandler initiationTaskHandlerService;

    @BeforeEach
    void setup() {
        dueDateService = new DueDateService(holidayService);

        initiationTaskHandlerService = new InitiationCaseEventHandler(
            serviceAuthGenerator,
            workflowApiClient,
            idempotencyKeyGenerator,
            isoDateFormatter,
            dueDateService,
            objectMapper,
            delayUntilConfigurator
        );

        eventInstanceId = UUID.randomUUID().toString();

        when(holidayService.isWeekend(any(ZonedDateTime.class))).thenCallRealMethod();
    }

    @DisplayName(" (No delayDuration, No delayUntil)")
    @Test
    void should_use_default_delay_duration_when_both_delay_duration_and_delay_until_are_not_present() {

        int delayDuration = 2;
        InitiateEvaluateResponse initiateEvaluateResponse = InitiateEvaluateResponse.builder()
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(delayDuration))
            .workingDaysAllowed(null)
            .processCategories(dmnStringValue("caseProgression"))
            .delayUntil(null)
            .build();

        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        when(isoDateFormatter.formatToZone(LocalDateTime.parse(EVENT_DATE)))
            .thenReturn(eventDateTime);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(true);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration + 1)))
            .thenReturn(false);

        when(serviceAuthGenerator.generate())
            .thenReturn(SERVICE_AUTH_TOKEN);

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .eventTimeStamp(LocalDateTime.parse(EVENT_DATE))
            .caseId("some case reference")
            .build();

        List<InitiateEvaluateResponse> results = List.of(initiateEvaluateResponse);

        initiationTaskHandlerService.handle(results, eventInformation);

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            "2022-07-22T16:00:00",
            "2022-07-22T16:00:00",
            0
        ));

    }

    @DisplayName(" (delayDuration, No delayUntil)")
    @Test
    void should_use_delay_duration_when_delay_duration_is_present_but_not_delay_until() {

        int delayDuration = 2;
        InitiateEvaluateResponse initiateEvaluateResponse = InitiateEvaluateResponse.builder()
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(delayDuration))
            .workingDaysAllowed(dmnIntegerValue(2))
            .processCategories(dmnStringValue("caseProgression"))
            .delayUntil(null)
            .build();

        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        when(isoDateFormatter.formatToZone(LocalDateTime.parse(EVENT_DATE)))
            .thenReturn(eventDateTime);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(true);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration + 1)))
            .thenReturn(false);

        when(serviceAuthGenerator.generate())
            .thenReturn(SERVICE_AUTH_TOKEN);

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .eventTimeStamp(LocalDateTime.parse(EVENT_DATE))
            .caseId("some case reference")
            .build();

        List<InitiateEvaluateResponse> results = List.of(initiateEvaluateResponse);

        initiationTaskHandlerService.handle(results, eventInformation);

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            "2022-07-26T16:00:00",
            "2022-07-22T16:00:00",
            2
        ));

    }

    @DisplayName(" (No delayDuration,  delayUntil)")
    @Test
    void should_use_delay_until_when_delay_duration_is_absent_and_delay_until_present() {

        int delayDuration = 2;
        InitiateEvaluateResponse initiateEvaluateResponse = InitiateEvaluateResponse.builder()
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(delayDuration))
            .workingDaysAllowed(null)
            .processCategories(dmnStringValue("caseProgression"))
            .delayUntil(new DmnValue<>(
                DelayUntilObject.builder()
                    .delayUntilOrigin("2022-10-23T16:00")
                    .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
                    .delayUntilIntervalDays(5)
                    .delayUntilTime("16:00").build(),
                null
            ))
            .build();

        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        when(isoDateFormatter.formatToZone(LocalDateTime.parse(EVENT_DATE)))
            .thenReturn(eventDateTime);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(true);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration + 1)))
            .thenReturn(false);

        when(serviceAuthGenerator.generate())
            .thenReturn(SERVICE_AUTH_TOKEN);

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .eventTimeStamp(LocalDateTime.parse(EVENT_DATE))
            .caseId("some case reference")
            .build();

        List<InitiateEvaluateResponse> results = List.of(initiateEvaluateResponse);

        initiationTaskHandlerService.handle(results, eventInformation);

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            "2022-10-28T16:00:00",
            "2022-10-28T16:00:00",
            0
        ));

    }

    @DisplayName(" (delayDuration,  delayUntil)")
    @Test
    void should_use_delay_until_when_both_delay_duration_and_delay_until_present() {

        int delayDuration = 2;
        InitiateEvaluateResponse initiateEvaluateResponse = InitiateEvaluateResponse.builder()
            .name(dmnStringValue("Process Application"))
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(delayDuration))
            .workingDaysAllowed(dmnIntegerValue(2))
            .processCategories(dmnStringValue("caseProgression"))
            .delayUntil(new DmnValue<>(
                DelayUntilObject.builder().delayUntil("2022-10-23").delayUntilTime("16:00").build(),
                null
            ))
            .build();

        ZonedDateTime eventDateTime =
            ZonedDateTime.of(
                2022, 7, 19,
                9, 0, 0, 0,
                ZoneId.systemDefault()
            );

        when(isoDateFormatter.formatToZone(LocalDateTime.parse(EVENT_DATE)))
            .thenReturn(eventDateTime);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration)))
            .thenReturn(true);

        when(holidayService.isHoliday(eventDateTime.plusDays(delayDuration + 1)))
            .thenReturn(false);

        when(serviceAuthGenerator.generate())
            .thenReturn(SERVICE_AUTH_TOKEN);

        when(idempotencyKeyGenerator.generateIdempotencyKey(eventInstanceId, "processApplication"))
            .thenReturn("idempotencyKey1");

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId(eventInstanceId)
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .eventTimeStamp(LocalDateTime.parse(EVENT_DATE))
            .caseId("some case reference")
            .build();

        List<InitiateEvaluateResponse> results = List.of(initiateEvaluateResponse);

        initiationTaskHandlerService.handle(results, eventInformation);

        verify(workflowApiClient, Mockito.times(1))
            .sendMessage(eq(SERVICE_AUTH_TOKEN), sendMessageRequestCaptor.capture());

        assertThat(sendMessageRequestCaptor.getAllValues().get(0)).isEqualTo(getExpectedSendMessageRequest(
            "idempotencyKey1",
            "Process Application",
            "processApplication",
            "__processCategory__caseProgression",
            "2022-10-25T16:00:00",
            "2022-10-23T16:00:00",
            2
        ));

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
                entry("caseTypeId", dmnStringValue("some case type")),
                entry("dueDate", dmnStringValue(dueDate)),
                entry("workingDaysAllowed", dmnIntegerValue(workingDays)),
                entry("jurisdiction", dmnStringValue("some jurisdiction")),
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
}

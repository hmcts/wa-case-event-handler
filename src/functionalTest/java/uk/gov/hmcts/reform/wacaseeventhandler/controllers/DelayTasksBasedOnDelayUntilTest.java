package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.azure.messaging.servicebus.ServiceBusMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.CamundaProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.CreatorObjectMapper.asJsonString;

@Slf4j
public class DelayTasksBasedOnDelayUntilTest extends SpringBootFunctionalBaseTest {


    private TestAuthenticationCredentials caseworkerCredentials;
    private LocalDateTime eventTimeStamp;

    private void sendMessage(String caseId,
                             String event,
                             String previousStateId,
                             String newStateId,
                             boolean taskDelay,
                             String jurisdictionId,
                             String caseTypeId) {

        if (taskDelay) {
            eventTimeStamp = LocalDateTime.now().plusSeconds(2);
        }
        EventInformation eventInformation = getEventInformation(
            caseId,
            event,
            previousStateId,
            newStateId,
            eventTimeStamp,
            jurisdictionId,
            caseTypeId
        );

        if (publisher != null) {
            publishMessageToTopic(eventInformation);
            waitSeconds(2);
        } else {
            callRestEndpoint(s2sToken, eventInformation);
        }
    }

    private EventInformation getEventInformation(String caseId,
                                                 String event,
                                                 String previousStateId,
                                                 String newStateId,
                                                 LocalDateTime localDateTime,
                                                 String jurisdictionId,
                                                 String caseTypeId) {
        String appealType = event.equals("submitAppeal") ? "deprivation" : "";

        return EventInformation.builder()
            .eventInstanceId(UUID.randomUUID().toString())
            .eventTimeStamp(localDateTime)
            .caseId(caseId)
            .jurisdictionId(jurisdictionId)
            .caseTypeId(caseTypeId)
            .eventId(event)
            .newStateId(newStateId)
            .previousStateId(previousStateId)
            .additionalData(setAdditionalData(appealType, "Adjourn"))
            .userId("some user Id")
            .build();
    }

    private void callRestEndpoint(String s2sToken, EventInformation eventInformation) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .body(asJsonString(eventInformation))
            .when()
            .post("/messages")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Before
    public void setup() {
        eventTimeStamp = LocalDateTime.now().minusDays(1);
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        common.cleanUpTask(caseworkerCredentials.getHeaders(), caseIds);
    }

    @Test
    public void should_create_delay_task_using_interval_for_event_delayUntilInterval() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "delayUntilInterval",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        CamundaProcessVariables camundaProcessVariables = findProcessVariablesByCaseId(caseId);
        camundaProcessVariables.getProcessVariablesMap()
            .forEach((key, value) -> log.info("Process variable is: {}, {}", key, value.getValue()));

        Map<String, DmnValue<?>> processVariables = camundaProcessVariables.getProcessVariablesMap();
        assertThat(processVariables.get("delayUntil").getValue()).isEqualTo("2023-01-03T18:00:00");

    }

    @Test
    public void should_create_delay_task_for_event_delayUntilDate() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "delayUntilDate",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        CamundaProcessVariables camundaProcessVariables = findProcessVariablesByCaseId(caseId);
        Map<String, DmnValue<?>> processVariables = camundaProcessVariables.getProcessVariablesMap();
        log.info("Process variables are {}", processVariables);
        String expected = LocalDateTime.now().withHour(16).withMinute(0).withSecond(0)
                .truncatedTo(ChronoUnit.MINUTES)
                + ":00";
        assertThat(processVariables.get("delayUntil").getValue()).isEqualTo(expected);

    }

    @Test
    public void should_create_delay_task_for_event_delayUntilTime() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "delayUntilTime",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        CamundaProcessVariables camundaProcessVariables = findProcessVariablesByCaseId(caseId);
        Map<String, DmnValue<?>> processVariables = camundaProcessVariables.getProcessVariablesMap();
        log.info("Process variables are {}", processVariables);
        String expected = LocalDateTime.now().withHour(16).withMinute(0).withSecond(0)
                .truncatedTo(ChronoUnit.MINUTES)
                + ":00";
        assertThat(processVariables.get("delayUntil").getValue()).isEqualTo(expected);

    }

    @Test
    public void should_create_delay_task_for_event_delayUntilDateTime() {

        String caseId = getWaCaseId();

        sendMessage(
            caseId,
            "delayUntilDateTime",
            "",
            "",
            false,
            "WA",
            "WaCaseType"
        );

        CamundaProcessVariables camundaProcessVariables = findProcessVariablesByCaseId(caseId);
        Map<String, DmnValue<?>> processVariables = camundaProcessVariables.getProcessVariablesMap();
        //truncatedTo does not keep the seconds if they are equal to zero
        String expected = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0)
                .truncatedTo(ChronoUnit.MINUTES)
                + ":00";
        assertThat(processVariables.get("delayUntil").getValue()).isEqualTo(expected);

    }

    private CamundaProcessVariables findProcessVariablesByCaseId(String caseId) {

        log.info("Finding process for caseId = {}", caseId);

        Set<String> processes = common.getProcesses(caseworkerCredentials.getHeaders(), caseId);
        log.info("No of process for case id is {}", processes.size());
        log.info("Process is {}", processes.iterator().next());
        return processes.stream()
            .findFirst()
            .map(key -> common.getProcessesInstanceVariables(caseworkerCredentials.getHeaders(), key))
            .orElseThrow(() -> new RuntimeException("Process instance is not yet created, Try again."));
    }

    private void publishMessageToTopic(EventInformation eventInformation) {
        String jsonMessage = asJsonString(eventInformation);
        ServiceBusMessage message = new ServiceBusMessage(jsonMessage.getBytes());
        message.setSessionId(eventInformation.getCaseId());

        publisher.sendMessage(message);
    }

    private AdditionalData setAdditionalData(String appealType, String lastModifiedApplicationType) {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of(
                "dateDue", "",
                "uniqueId", "",
                "directionType", ""
            ),
            "appealType", appealType,
            "lastModifiedApplication", Map.of("type", lastModifiedApplicationType,
                                              "decision", ""
            )

        );

        return AdditionalData.builder()
            .data(dataMap)
            .build();
    }

}

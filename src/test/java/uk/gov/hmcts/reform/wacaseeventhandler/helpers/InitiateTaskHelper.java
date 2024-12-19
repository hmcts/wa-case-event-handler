package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnMapValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

public final class InitiateTaskHelper {

    private InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateEvaluateResponse> buildInitiateTaskDmnResponse() {
        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("processApplication"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .name(dmnStringValue("Process Application"))
            .build();

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest buildInitiateTaskDmnRequest(String dueDate,
                                                                 Map<String, Object> appealType) {
        DmnValue<String> eventId = dmnStringValue("submitAppeal");
        DmnValue<String> postEventState = dmnStringValue("");
        DmnValue<Map<String, Object>> mapDmnValue = dmnMapValue(appealType);
        DmnValue<String> now = dmnStringValue(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        DmnValue<String> directionDueDate = dmnStringValue(dueDate);
        Map<String, DmnValue<?>> variables = Map.of(
            "eventId", eventId,
            "postEventState", postEventState,
            "additionalData", mapDmnValue,
            "now", now,
            "directionDueDate", directionDueDate
        );

        return new EvaluateDmnRequest(variables);
    }


    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategies.UPPER_CAMEL_CASE).writeValueAsString(obj);
    }

    public static String asJsonString(final Map<String, String> obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public static EventInformation validAdditionalData() {

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-06"),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);
    }

    public static EventInformation withoutAppealType() {

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-06")
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);
    }

    public static EventInformation withoutDirectionDueDate() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("lastModifiedDirection", null);
        dataMap.put("appealType", "protection");

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);
    }

    public static EventInformation withoutLastModifiedDirection() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("appealType", "protection");

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);

    }

    public static EventInformation withEmptyDirectionDueDate() {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", ""),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);
    }

    private static EventInformation getEventInformation(AdditionalData additionalData) {
        return EventInformation.builder()
            .eventInstanceId("eventInstanceId")
            .eventId("submitAppeal")
            .newStateId("")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .caseId("some case reference")
            .eventTimeStamp(LocalDateTime.parse("2020-03-29T10:53:36.530377"))
            .additionalData(additionalData)
            .build();
    }
}

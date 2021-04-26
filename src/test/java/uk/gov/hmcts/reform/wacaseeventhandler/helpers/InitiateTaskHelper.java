package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InitiateTaskHelper {

    private InitiateTaskHelper() {
        //not called
    }

    public static EvaluateDmnResponse<InitiateEvaluateResponse> buildInitiateTaskDmnResponse() {
        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(new DmnStringValue("processApplication"))
            .group(new DmnStringValue("TCW"))
            .delayDuration(new DmnIntegerValue(2))
            .workingDaysAllowed(new DmnIntegerValue(2))
            .name(new DmnStringValue("Process Application"))
            .build();

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest<InitiateEvaluateRequest> buildInitiateTaskDmnRequest(String dueDate,
                                                                                          String appealTypeValue) {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        DmnStringValue appealType = new DmnStringValue(appealTypeValue);
        DmnStringValue now = new DmnStringValue(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        DmnStringValue directionDueDate = new DmnStringValue(dueDate);
        InitiateEvaluateRequest initiateEvaluateRequestVariables =
            new InitiateEvaluateRequest(
                eventId,
                postEventState,
                appealType,
                now,
                directionDueDate
            );

        return new EvaluateDmnRequest<>(initiateEvaluateRequestVariables);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().setPropertyNamingStrategy(
            PropertyNamingStrategy.UPPER_CAMEL_CASE).writeValueAsString(obj);
    }

    public static String asJsonString(final Map<String, String> obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public static EventInformation validAdditionalData() {

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("directionDueDate", "2021-04-06"),
            "appealType", "protection"
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
            "lastModifiedDirection", Map.of("directionDueDate", ""),
            "appealType", ""
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

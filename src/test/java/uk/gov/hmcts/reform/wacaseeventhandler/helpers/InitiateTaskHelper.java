package uk.gov.hmcts.reform.wacaseeventhandler.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.assertj.core.util.Maps;
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
            .workingDaysAllowed(new DmnIntegerValue(2))
            .name(new DmnStringValue("Process Application"))
            .build();

        return new EvaluateDmnResponse<>(List.of(result));
    }

    public static EvaluateDmnRequest<InitiateEvaluateRequest> buildInitiateTaskDmnRequest(String dueDate) {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        DmnStringValue appealType = new DmnStringValue("protection");
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

    public static EventInformation validAdditionalData() {

            final AdditionalData additionalData = AdditionalData.builder()
                .data(Map.of("directionDueDate", "2021-04-06"))
                .build();

            return getEventInformation(additionalData);
    }

    public static EventInformation withoutDirectionDueDate() {

        final AdditionalData additionalData = AdditionalData.builder()
            .data(Maps.newHashMap("lastModifiedDirection", null)).build();

        return getEventInformation(additionalData);
    }

    public static EventInformation withoutLastModifiedDirection() {
        final AdditionalData additionalData = AdditionalData.builder()
            .data(null).build();

        return getEventInformation(additionalData);

    }

    public static EventInformation withEmptyDirectionDueDate() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String lastModifiedDirection = "{\"directionDueDate\": \"\"}";
            JsonNode jsonNode = objectMapper.readTree(lastModifiedDirection);
            Map<String, JsonNode> dataMap = Maps.newHashMap("lastModifiedDirection", jsonNode);

            final AdditionalData additionalData = AdditionalData.builder()
                .data(dataMap)
                .build();

            return getEventInformation(additionalData);

        } catch (JsonProcessingException exp) {
            return null;
        }
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

package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import pl.pojo.tester.api.assertion.Method;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@RunWith(SpringRunner.class)
@JsonTest
class EventInformationTest {

    @Autowired
    private JacksonTester<EventInformation> jacksonTester;

    @Test
    void isWellImplemented() {

        final Class<?> classUnderTest = EventInformation.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }

    @Test
    void deserialize_as_expected() throws IOException {
        ObjectContent<EventInformation> eventInformationObjectContent =
            jacksonTester.read("expected-event-information-from-ccd.json");

        eventInformationObjectContent.assertThat().isEqualToComparingFieldByField(eventInformation(null));
    }

    @Test
    void serialize_as_expected() throws IOException {
        EventInformation validEventInformation = eventInformation(null);
        JsonContent<EventInformation> eventInformationJsonContent = jacksonTester.write(validEventInformation);

        assertThat(eventInformationJsonContent).isEqualToJson("valid-event-information.json");
    }

    @Test
    void deserialize_as_expected_with_additional_data() throws IOException {
        ObjectContent<EventInformation> eventInformationObjectContent =
            jacksonTester.read("expected-event-information-additional-data.json");

        eventInformationObjectContent.assertThat().isEqualToComparingFieldByField(eventInformation(additionalData()));
    }

    @Test
    void serialize_with_additional_data_as_expected() throws IOException {
        EventInformation validEventInformation = eventInformation(additionalData());
        JsonContent<EventInformation> eventInformationJsonContent = jacksonTester.write(validEventInformation);

        assertThat(eventInformationJsonContent).isEqualToJson("valid-event-information-additional-data.json");
    }

    private EventInformation eventInformation(AdditionalData additionalData) {
        return EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(LocalDateTime.parse("2020-12-07T17:39:22.232622"))
            .caseId("some case reference")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .additionalData(additionalData)
            .build();
    }

    private AdditionalData additionalData() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-08"),
            "appealType", "protection"
        );

        String definition = "{\n"
                            + "        \"type\": \"Complex\",\n"
                            + "        \"subtype\": \"lastModifiedDirection\",\n"
                            + "        \"typeDef\": {\n"
                            + "          \"dateDue\": {\n"
                            + "            \"type\": \"SimpleDate\",\n"
                            + "            \"subtype\": \"Date\",\n"
                            + "            \"typeDef\": null,\n"
                            + "            \"originalId\": \"dateDue\"\n"
                            + "          }\n"
                            + "        },\n"
                            + "        \"originalId\": \"lastModifiedDirection\"\n"
                            + "      }";
        JsonNode jsonNode = objectMapper.readTree(definition);
        Map<String, JsonNode> definitionMap = Maps.newHashMap("lastModifiedDirection", jsonNode);

        return AdditionalData.builder()
            .data(dataMap)
            .definition(definitionMap)
            .build();
    }

}

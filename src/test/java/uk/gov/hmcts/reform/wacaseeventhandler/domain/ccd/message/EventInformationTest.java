package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void isWellImplemented() {

        final Class<?> classUnderTest = EventInformation.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();

    }

    @Test
    void deserialize_as_expected() throws IOException {
        ObjectContent<EventInformation> eventInformationObjectContent =
            jacksonTester.read("expected-event-information-from-ccd.json");

        eventInformationObjectContent.assertThat()
            .usingRecursiveComparison()
            .isEqualTo(eventInformation(null));
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

        eventInformationObjectContent.assertThat()
            .usingRecursiveComparison()
            .isEqualTo(eventInformation(additionalData()));
    }

    @Test
    void metadata_hold_until_round_trips_as_an_iso_string() {
        assertThat(objectMapper.getClass().getPackageName()).startsWith("tools.jackson.");

        EventInformationMetadata metadata = new EventInformationMetadata(
            Map.of("key", "value"),
            LocalDateTime.parse("2020-12-07T17:39:22.232622")
        );

        String json = objectMapper.writeValueAsString(metadata);

        assertThat(json).contains("\"HoldUntil\"");
        assertThat(json).contains("2020-12-07T17:39:22.232622");
        assertThat(json).doesNotContain("holdUntil");
        assertThat(objectMapper.readValue(json, EventInformationMetadata.class)).isEqualTo(metadata);
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

    private AdditionalData additionalData() throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-08"),
            "appealType", "protection"
        );

        String definition = """
        {
            "type": "Complex",
            "subtype": "lastModifiedDirection",
            "typeDef": {
              "dateDue": {
                "type": "SimpleDate",
                "subtype": "Date",
                "typeDef": null,
                "originalId": "dateDue"
              }
            },
            "originalId": "lastModifiedDirection"
        }""";
        JsonNode jsonNode = objectMapper.readTree(definition);
        Map<String, JsonNode> definitionMap = Maps.newHashMap("lastModifiedDirection", jsonNode);

        return AdditionalData.builder()
            .data(dataMap)
            .definition(definitionMap)
            .build();
    }

}

package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@ActiveProfiles("local")
public class EventInformationTest {

    @Autowired
    private JacksonTester<EventInformation> jacksonTester;

    private final EventInformation validEventInformation = EventInformation.builder()
        .eventInstanceId("some event instance Id")
        .caseReference("some case reference")
        .jurisdictionId("somme jurisdiction Id")
        .caseTypeId("some case type Id")
        .eventId("some event Id")
        .newStateId("some new state Id")
        .userId("some user Id")
        .build();

    @Test
    public void deserialize_as_expected() throws IOException {
        ObjectContent<EventInformation> eventInformationObjectContent =
            jacksonTester.read("valid-event-information.json");

        eventInformationObjectContent.assertThat().isEqualTo(validEventInformation);
    }

    @Test
    public void serialize_as_expected() throws IOException {

        JsonContent<EventInformation> eventInformationJsonContent = jacksonTester.write(validEventInformation);

        assertThat(eventInformationJsonContent).isEqualToJson("valid-event-information.json");
    }

}

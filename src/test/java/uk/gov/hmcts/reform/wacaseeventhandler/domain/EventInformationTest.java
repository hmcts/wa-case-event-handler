package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

@RunWith(SpringRunner.class)
@JsonTest
@ActiveProfiles("local")
public class EventInformationTest {

    private final Class classToTest = EvaluateDmnResponse.class;

    @Autowired
    private JacksonTester<EventInformation> jacksonTester;

    private EventInformation validEventInformation;

    @Before
    public void setUp() {
        String fixedDate = "2020-12-07T17:39:22.232622";
        validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(LocalDateTime.parse(fixedDate))
            .caseId("some case reference")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();
    }

    @Test
    public void deserialize_as_expected() throws IOException {
        ObjectContent<EventInformation> eventInformationObjectContent =
            jacksonTester.read("expected-event-information-from-ccd.json");

        eventInformationObjectContent.assertThat().isEqualTo(validEventInformation);
    }

    @Test
    public void serialize_as_expected() throws IOException {
        JsonContent<EventInformation> eventInformationJsonContent = jacksonTester.write(validEventInformation);

        assertThat(eventInformationJsonContent).isEqualToJson("valid-event-information.json");
    }

    @Test
    public void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }

}

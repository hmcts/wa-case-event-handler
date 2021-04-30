package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class EventInformationTest {

    private final Class classToTest = EventInformation.class;

    @Test
    void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }
}

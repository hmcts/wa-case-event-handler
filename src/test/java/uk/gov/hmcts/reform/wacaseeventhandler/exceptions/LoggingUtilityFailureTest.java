package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class LoggingUtilityFailureTest {
    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = LoggingUtilityFailure.class;
        assertPojoMethodsFor(classUnderTest)
            .testing(Method.CONSTRUCTOR)
            .areWellImplemented();
    }

}

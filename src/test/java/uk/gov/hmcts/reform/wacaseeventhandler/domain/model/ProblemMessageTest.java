package uk.gov.hmcts.reform.wacaseeventhandler.domain.model;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class ProblemMessageTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = ProblemMessage.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

}

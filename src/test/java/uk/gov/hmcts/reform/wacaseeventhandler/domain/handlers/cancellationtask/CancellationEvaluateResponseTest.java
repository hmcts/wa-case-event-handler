package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class CancellationEvaluateResponseTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = CancellationEvaluateResponse.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }


}

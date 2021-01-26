package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.common;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

public class EvaluateDmnResponseTest {

    private final Class classToTest = EvaluateDmnResponse.class;

    @Test
    void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.common;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

public class DmnIntegerValueTest {

    private final Class classToTest = DmnIntegerValue.class;

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

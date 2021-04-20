package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.AdditionalData;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

public class AdditionalDataTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = AdditionalData.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class AdditionalDataTest {

    @Test
    void isWellImplemented() {

        final Class<?> classUnderTest = AdditionalData.class;

        assertPojoMethodsFor(classUnderTest)
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }
}

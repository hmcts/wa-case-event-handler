package uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.FieldPredicate;
import pl.pojo.tester.api.assertion.Method;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateProcessVariables;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class InitiateProcessVariablesTest {

    @Test
    void isWellImplemented() {
        final Class<?> classUnderTest = InitiateProcessVariables.class;

        assertPojoMethodsFor(classUnderTest, FieldPredicate.exclude("caseId"))
            .testing(Method.GETTER)
            .testing(Method.CONSTRUCTOR)
            .testing(Method.TO_STRING)
            .areWellImplemented();
    }

}

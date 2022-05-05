package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class UkHolidayDatesTest {

    private final Class classToTest = UkHolidayDates.class;

    @Test
    void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }
}

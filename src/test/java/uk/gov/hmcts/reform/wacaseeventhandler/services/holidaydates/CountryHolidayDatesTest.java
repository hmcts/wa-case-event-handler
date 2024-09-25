package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.assertion.Method;

import static pl.pojo.tester.api.assertion.Assertions.assertPojoMethodsFor;

class CountryHolidayDatesTest {

    private final Class classToTest = CountryHolidayDates.class;

    @Test
    void isWellImplemented() {
        assertPojoMethodsFor(classToTest)
            .testing(Method.GETTER)
            .testing(Method.EQUALS)
            .testing(Method.HASH_CODE)
            .testing(Method.TO_STRING)
            .areWellImplemented();

    }
}

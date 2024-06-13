package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import feign.RequestLine;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.BankHolidays;

public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll();
}

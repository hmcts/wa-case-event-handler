package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class CcdMessageLogger {

    private String caseId;
    private String caseTypeId;
    private String eventId;
    private String jurisdictionId;
}

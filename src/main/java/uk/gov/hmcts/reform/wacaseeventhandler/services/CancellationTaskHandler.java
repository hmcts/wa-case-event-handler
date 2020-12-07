package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {
    @Override
    public boolean canHandle(EventInformation eventInformation) {
        return false;
    }

    @Override
    public void handle() {
        // empty for now
    }
}

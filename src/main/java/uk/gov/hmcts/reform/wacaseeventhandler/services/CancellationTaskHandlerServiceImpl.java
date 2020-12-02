package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(1)
public class CancellationTaskHandlerServiceImpl implements CaseEventHandlerService {
    @Override
    public boolean canHandle() {
        return false;
    }

    @Override
    public void handle() {

    }
}

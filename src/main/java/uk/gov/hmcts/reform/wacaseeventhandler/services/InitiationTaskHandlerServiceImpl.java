package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Order(3)
public class InitiationTaskHandlerServiceImpl implements CaseEventHandlerService {
    @Override
    public boolean canHandle() {
        log.info("InitiationTaskHandlerServiceImpl.canHandle.false");
        return false;
    }

    @Override
    public void handle() {
        log.info("InitiationTaskHandlerServiceImpl.handle");
    }
}

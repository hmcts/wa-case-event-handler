package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(value = "azure.servicebus.enableASB", matchIfMissing = true, havingValue = "false")
public class LocalDeadLetterQueuePeekService implements DeadLetterQueuePeekService {

    private boolean response;

    @Override
    public void setResponse(boolean booleanResponse) {
        response = booleanResponse;
    }

    @Override
    public boolean isDeadLetterQueueEmpty() {
        log.info("inside LocalDeadLetterQueuePeekService.isDeadLetterQueueEmpty(), response is " + response);
        return response;
    }
}

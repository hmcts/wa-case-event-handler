package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@ConditionalOnProperty(value = "azure.servicebus.enableASB", havingValue = "true")
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class AsbEnabledDeadLetterQueuePeekService implements DeadLetterQueuePeekService {

    private final ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient;

    public AsbEnabledDeadLetterQueuePeekService(ServiceBusConfiguration serviceBusConfiguration) {
        this.serviceBusReceiverAsyncClient =
                serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueAsyncClient();
    }

    @Override
    public boolean isDeadLetterQueueEmpty() {
        log.info("inside AsbEnabledDeadLetterQueuePeekService.isDeadLetterQueueEmpty()");
        AtomicBoolean deadLetterQueueEmpty = new AtomicBoolean(true);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        serviceBusReceiverAsyncClient.peekMessage(1).subscribe(
            message -> {
                log.trace("Peeked at Message Id {} from DLQ: ", message.getMessageId());
                deadLetterQueueEmpty.set(false);
            },
            error -> log.error("Error occurred while receiving message: " + error),
            () -> log.trace("Peek at DLQ complete.")
        );

        try {
            countDownLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted exception received", e);
        }

        return deadLetterQueueEmpty.get();
    }

    @Override
    public void setResponse(boolean response) {
        log.trace("Not setting response when running asb enabled tests");
    }
}

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
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DeadLetterQueuePeekService {

    private final ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient;

    public DeadLetterQueuePeekService(ServiceBusConfiguration serviceBusConfiguration) {
        this.serviceBusReceiverAsyncClient =
                serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueAsyncClient();
    }

    public boolean isDeadLetterQueueEmpty() {
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
}

package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

@Slf4j
@Service
@ConditionalOnProperty("azure.servicebus.enableASB")
public class DeadLetterQueuePeekService {

    private final ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient;

    public DeadLetterQueuePeekService(ServiceBusConfiguration serviceBusConfiguration) {
        this.serviceBusReceiverAsyncClient =
                serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueAsyncClient();
    }

    public boolean isDeadLetterQueueEmpty() {
        ServiceBusReceivedMessage serviceBusReceivedMessage = serviceBusReceiverAsyncClient.peekMessage(1).block();
        return serviceBusReceivedMessage == null;
    }
}

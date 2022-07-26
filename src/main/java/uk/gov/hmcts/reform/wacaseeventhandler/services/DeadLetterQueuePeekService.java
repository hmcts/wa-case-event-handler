package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

@Slf4j
@Service
@ConditionalOnProperty("azure.servicebus.enableASB-DLQ")
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DeadLetterQueuePeekService {

    private final ServiceBusReceiverClient serviceBusReceiverClient;

    public DeadLetterQueuePeekService(ServiceBusConfiguration serviceBusConfiguration) {
        this.serviceBusReceiverClient =
                serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver();
    }

    public boolean isDeadLetterQueueEmpty() {
        ServiceBusReceivedMessage message = serviceBusReceiverClient.peekMessage(1);
        log.info("Is dead letter queue empty {}", message == null);
        return message == null;
    }
}

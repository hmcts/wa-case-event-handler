package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import java.time.Duration;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdEventConsumer implements Runnable {

    private final String hostName;
    private final String topicName;
    private final String subscriptionName;
    private final int retryTime;

    private final CcdMessageProcessor ccdMessageProcessor;

    public CcdEventConsumer(CcdMessageProcessor ccdMessageProcessor,
                            @Value("${azure.servicebus.host-name}") String hostName,
                            @Value("${azure.servicebus.topic-name}") String topicName,
                            @Value("${azure.servicebus.subscription-name}") String subscriptionName,
                            @Value("${azure.servicebus.retry-duration}") int retryTime
    ) {
        this.ccdMessageProcessor = ccdMessageProcessor;
        this.hostName = hostName;
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
        this.retryTime = retryTime;
    }

    @Override
    public void run() {
        try (ServiceBusSessionReceiverClient sessionReceiver = createSessionReceiver()) {
            while (true) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1)
                .forEach(
                    message -> {
                        try {
                            log.info("Message consumed: %s from Thread: %s",
                                              new String(message.getBody().toBytes()),
                                              Thread.currentThread().getName()
                            );
                            ccdMessageProcessor.processMesssage(new String(message.getBody().toBytes()));
                            receiver.complete(message);
                        } catch (JsonProcessingException exp) {
                            log.error("Unable to parse event", exp);
                        }
                    });
        } catch (IllegalStateException exp) {
            log.error("Error occurred while closing the session", exp);
        }
    }

    private ServiceBusSessionReceiverClient createSessionReceiver() {
        return new ServiceBusClientBuilder()
            .connectionString(hostName)
            .retryOptions(new AmqpRetryOptions().setTryTimeout(
                Duration.ofSeconds(retryTime)))
            .sessionReceiver()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .buildClient();
    }

}

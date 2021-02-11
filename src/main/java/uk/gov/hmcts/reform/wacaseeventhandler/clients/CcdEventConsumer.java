package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdEventConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final CcdMessageProcessor ccdMessageProcessor;

    public CcdEventConsumer(ServiceBusConfiguration serviceBusConfiguration,
                            CcdMessageProcessor ccdMessageProcessor
    ) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.ccdMessageProcessor = ccdMessageProcessor;
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        try (ServiceBusSessionReceiverClient sessionReceiver = serviceBusConfiguration.createSessionReceiver()) {
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
                            ccdMessageProcessor.processMessage(new String(message.getBody().toBytes()));
                            receiver.complete(message);
                        } catch (JsonProcessingException exp) {
                            log.error("Unable to parse event", exp);
                        }
                    });
        } catch (IllegalStateException exp) {
            log.error("Error occurred while closing the session", exp);
        }
    }


}

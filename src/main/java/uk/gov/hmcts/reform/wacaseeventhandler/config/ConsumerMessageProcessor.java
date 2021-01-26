package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ConsumerMessageProcessor {

    @Bean
    public Consumer<ServiceBusReceivedMessageContext> receiveMessageWithSession() {
        Consumer<ServiceBusReceivedMessageContext> onMessage = context -> {
            ServiceBusReceivedMessage message = context.getMessage();
            processMessage(message);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        return onMessage;
    }

    private static boolean processMessage(ServiceBusReceivedMessage message) {
        System.out.printf("Session: %s. Sequence #: %s. Contents: %s%n", message.getSessionId(),
                          message.getSequenceNumber(), message.getBody());
        return true;
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ConsumerMessageErrorProcessor {

    @Bean
    public Consumer<ServiceBusErrorContext> errorHandler() {
        Consumer<ServiceBusErrorContext> onError = context -> {
            System.out.printf("Error when receiving messages from namespace: '%s'. Entity: '%s'%n",
                              context.getFullyQualifiedNamespace(), context.getEntityPath());

            if (context.getException() instanceof ServiceBusException) {
                ServiceBusException exception = (ServiceBusException) context.getException();
                System.out.printf("Error source: %s, reason %s%n", context.getErrorSource(),
                                  exception.getReason());
            } else {
                System.out.printf("Error occurred: %s%n", context.getException());
            }
        };
        return onError;
    }
}

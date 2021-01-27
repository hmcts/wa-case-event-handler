package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import java.util.function.Consumer;

@Slf4j
@Component
@Profile("aat")
public class CcdEventMessageConsumer {

    private final String host;
    private final String topic;
    private final String subscription;

    private final CcdMessageProcessor processor;

    public CcdEventMessageConsumer(CcdMessageProcessor processor,
                                   @Value("${azure.host}") String host,
                                   @Value("${azure.topic}") String topic,
                                   @Value("${azure.topic}") String subscription) {
        this.processor = processor;
        this.host = host;
        this.topic = topic;
        this.subscription = subscription;
    }

    @Bean
    public Consumer<ServiceBusReceivedMessageContext> consumeMessage() {
        return context -> processor.processMesssage(
            new String(context.getMessage().getBody().toBytes()));
    }

    @Bean
    public Consumer<ServiceBusErrorContext> handleError() {
        return context -> log.error(context.getEntityPath(), context.getException());
    }

}

package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty("azure.enableASB")
public class CcdEventMessageConsumer {

    private final CcdMessageProcessor processor;

    @Autowired
    public CcdEventMessageConsumer(CcdMessageProcessor processor) {
        this.processor = processor;
    }

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public Consumer<ServiceBusReceivedMessageContext> consumeMessageFromChannel() {
        return context -> readMessage(context.getMessage().getBody().toBytes());
    }

    public void readMessage(byte[] data) {
        try {
            processor.processMesssage(new String(data));
        } catch (JsonProcessingException exp) {
            // This should be sent to deadletter queue
            log.error("Error occured while parsing the incoming message", exp);
        }
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @Bean
    public Consumer<ServiceBusErrorContext> handleError() {
        // This should be send to deadletter queue
        return context -> log.error("Error occurred while receving message",
                                    context.getErrorSource(), context.getException());
    }

}

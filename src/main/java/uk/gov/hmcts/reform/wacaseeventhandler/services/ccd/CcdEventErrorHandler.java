package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class CcdEventErrorHandler {

    private final int retryAttempts;
    private final DeadLetterService deadLetterService;

    public CcdEventErrorHandler(DeadLetterService deadLetterService,
                                @Value("${azure.servicebus.retry-attempts}") int retryAttempts) {
        this.deadLetterService = deadLetterService;
        this.retryAttempts = retryAttempts;
    }


    public void handleJsonError(ServiceBusReceiverClient receiver,
                                ServiceBusReceivedMessage message,
                                JsonProcessingException ex) {
        log.error("Unable to parse incoming message with id '{}'", message.getMessageId(), ex);
        String messageData = new String(message.getBody().toBytes());

        receiver.deadLetter(message, deadLetterService.handleParsingError(messageData, ex.getMessage()));

        log.warn("Message with id '{}' was dead lettered", message.getMessageId(), ex);
    }

    public void handleApplicationError(ServiceBusReceiverClient receiver,
                                       ServiceBusReceivedMessage message,
                                       RestClientException ex) {
        log.error("Unable to process incoming message with id '{}'", message.getMessageId(), ex);

        final Long deliveryCount = message.getRawAmqpMessage().getHeader().getDeliveryCount();
        if (deliveryCount >= retryAttempts) {
            String messageData = new String(message.getBody().toBytes());

            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(messageData, ex.getMessage()));

            log.warn("Max delivery count reached. Message with id '{}' was dead lettered", message.getMessageId());

        } else {
            receiver.abandon(message);
            log.warn("Retrying message with id '{}'", message.getMessageId());
        }
    }

    public void handleGenericError(ServiceBusReceiverClient receiver,
                                   ServiceBusReceivedMessage message,
                                   Exception ex) {
        log.error("Unable to parse incoming message with id '{}'", message.getMessageId(), ex);
        String messageData = new String(message.getBody().toBytes());

        if (StringUtils.hasLength(ex.getMessage())) {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(messageData, "Unknown Error"));
        } else {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(messageData, ex.getMessage()));
        }
        log.warn("Unknown Error. Message with id '{}' was dead lettered", message.getMessageId(), ex);
    }

}

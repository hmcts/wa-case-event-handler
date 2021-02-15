package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CcdEventException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventLogger;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.DeadLetterService;

import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdEventConsumer implements Runnable {

    private static final String CASE_ID = "case_id";
    private static final String EVENT_ID = "event_id";
    private static final String JURISDICTION_ID = "jurisdiction_id";
    private static final String CASE_TYPE_ID = "case_type_id";

    private final int retryAttempts;
    private final ServiceBusConfiguration serviceBusConfiguration;
    private final CcdEventProcessor ccdEventProcessor;
    private final DeadLetterService deadLetterService;

    public CcdEventConsumer(ServiceBusConfiguration serviceBusConfiguration,
                            CcdEventProcessor ccdEventProcessor,
                            DeadLetterService deadLetterService,
                            @Value("${azure.servicebus.retry-attempts}") int retryAttempts
    ) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.ccdEventProcessor = ccdEventProcessor;
        this.deadLetterService = deadLetterService;
        this.retryAttempts = retryAttempts;
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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1)
                .forEach(
                    message -> {
                        final String loggerMsg = getLoggerMsg(message);

                        String incomingMessage = new String(message.getBody().toBytes());
                        try {
                            log.info(String.format("Processing case details: %s", loggerMsg));

                            ccdEventProcessor.processMesssage(incomingMessage);
                            receiver.complete(message);

                            log.info(String.format("Processing completed successfully: "
                                                       + "on case details %s", loggerMsg));
                        } catch (JsonProcessingException exp) {
                            handleJsonError(receiver, message, loggerMsg, incomingMessage, exp);
                        } catch (CcdEventException exp) {
                            handleApplicationError(receiver, message, loggerMsg, incomingMessage, exp);
                        }
                    });
        } catch (IllegalStateException exp) {
            log.error("Error occurred while closing the session", exp);
        }
    }

    private void handleJsonError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                 String loggerMsg, String incomingMessage, JsonProcessingException exp) {
        log.error(String.format("Unable to parse incoming message: %s on case details %s",
                                incomingMessage, loggerMsg
        ), exp);

        receiver.deadLetter(message, deadLetterService

            .handleParsingError(incomingMessage, exp.getMessage()));
        log.warn(String.format("Dead lettering: %s", loggerMsg));
    }

    private void handleApplicationError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                        String loggerMsg, String incomingMessage, RuntimeException exp) {
        log.error(String.format("Unable to process case details: %s", loggerMsg), exp);

        final Long deliveryCount = message.getRawAmqpMessage().getHeader().getDeliveryCount();
        if (deliveryCount >= retryAttempts) {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, exp.getMessage()));

            log.warn(String.format("Max delivery count reached. Dead Lettering: %s", loggerMsg));
        } else {
            receiver.abandon(message);
            log.warn(String.format("Retrying to process case details: %s", loggerMsg));
        }
    }

    private String getLoggerMsg(ServiceBusReceivedMessage message) {
        final Map<String, Object> msgProperties = message.getApplicationProperties();

        return CcdEventLogger.builder()
            .caseId((String) msgProperties.get(CASE_ID))
            .eventId((String) msgProperties.get(EVENT_ID))
            .jurisdictionId((String) msgProperties.get(JURISDICTION_ID))
            .caseTypeId((String) msgProperties.get(CASE_TYPE_ID))
            .build().toString();
    }

}

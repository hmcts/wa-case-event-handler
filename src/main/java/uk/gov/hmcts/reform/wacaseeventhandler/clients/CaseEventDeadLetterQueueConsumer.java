package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.MessageReceiver;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventErrorHandler;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CaseEventDeadLetterQueueConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final CcdEventErrorHandler ccdEventErrorHandler;
    private final MessageReceiver messageReceiver;

    public CaseEventDeadLetterQueueConsumer(ServiceBusConfiguration serviceBusConfiguration,
                                            CcdEventErrorHandler ccdEventErrorHandler,
                                            MessageReceiver messageReceiver
    ) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.ccdEventErrorHandler = ccdEventErrorHandler;
        this.messageReceiver = messageReceiver;
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        try (ServiceBusSessionReceiverClient sessionReceiver
                     = serviceBusConfiguration.createDeadLetterQueueSessionReceiver()) {
            while (true) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1)
                .forEach(
                    message -> {

                        String incomingMessage = new String(message.getBody().toBytes());
                        try {
                            log.info("Received DLQ message with id '{}'", message.getMessageId());

                            messageReceiver.processCaseEventDeadLetterQueue(incomingMessage);

                            receiver.complete(message);

                            log.info("DLQ Message with id '{}' handled successfully", message.getMessageId());
                        } catch (JsonProcessingException ex) {
                            ccdEventErrorHandler.handleJsonError(receiver, message, ex);
                        } catch (RestClientException ex) {
                            ccdEventErrorHandler.handleApplicationError(receiver, message, ex);
                        } catch (Exception ex) {
                            ccdEventErrorHandler.handleGenericError(receiver, message, ex);
                        }
                    });
        } catch (IllegalStateException ex) {
            log.info("Timeout: No DLQ messages received, waiting for next session.");
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }
}

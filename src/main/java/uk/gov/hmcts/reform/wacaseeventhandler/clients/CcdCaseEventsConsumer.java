package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@Profile("!functional & !local")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdCaseEventsConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final EventMessageReceiverService eventMessageReceiverService;

    public CcdCaseEventsConsumer(ServiceBusConfiguration serviceBusConfiguration,
                                 EventMessageReceiverService eventMessageReceiverService) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.eventMessageReceiverService = eventMessageReceiverService;
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        try (ServiceBusSessionReceiverClient sessionReceiver =
                     serviceBusConfiguration.createCcdCaseEventsSessionReceiver()) {
            while (true) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1).forEach(
                message -> {
                    try {
                        String messageId = message.getMessageId();
                        log.info("Received CCD Case Event message with id '{}'", messageId);

                        eventMessageReceiverService.handleCcdCaseEventAsbMessage(messageId,
                                new String(message.getBody().toBytes()));
                        receiver.complete(message);

                        log.info("CCD Case Event message with id '{}' handled successfully", messageId);
                    } catch (Exception ex) {
                        log.error("Error processing CCD Case Event message with id '{}' - "
                                + "will continue to complete message", message.getMessageId());
                        receiver.complete(message);
                    }
                });
        } catch (IllegalStateException ex) {
            log.info("Timeout: No CCD Case Event messages received waiting for next session.");
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }
}

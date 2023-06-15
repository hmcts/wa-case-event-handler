package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusException;
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
@ConditionalOnProperty("azure.servicebus.enableASB-DLQ")
@Profile("!functional & !local")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdCaseEventsConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final EventMessageReceiverService eventMessageReceiverService;
    private boolean keepRun = true;

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
            while (keepRun) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            if (receiver != null) {
                receiver.receiveMessages(1).forEach(
                    message -> {
                        try {
                            String messageId = message.getMessageId();
                            String sessionId = message.getSessionId();
                            log.info(
                                "Received CCD Case Event message with id '{}' and case id '{}'",
                                messageId,
                                sessionId
                            );

                            eventMessageReceiverService
                                .handleCcdCaseEventAsbMessage(messageId, sessionId,
                                                              new String(message.getBody().toBytes()));
                            receiver.complete(message);

                            log.info("CCD Case Event message with id '{}' handled successfully", messageId);
                        } catch (Exception ex) {
                            log.error(
                                "Error processing CCD Case Event message with id '{}' - "
                                    + "abandon the processing and ASB will re-deliver it",
                                message.getMessageId()
                            );
                            receiver.abandon(message);
                        }
                    });
            }
        } catch (IllegalStateException ex) {
            log.info("Timeout: No CCD Case Event messages received waiting for next session.");
        } catch (ServiceBusException ex) {
            log.error("Error occurred while receiving messages: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }

    public void stop() {
        keepRun = false;
    }
}

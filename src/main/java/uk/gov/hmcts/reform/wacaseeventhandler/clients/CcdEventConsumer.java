package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventErrorHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;
import uk.gov.hmcts.reform.wacaseeventhandler.util.UserIdParser;

import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_PROCESS;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@Profile("!functional & !local")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdEventConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final CcdEventProcessor ccdEventProcessor;
    private final CcdEventErrorHandler ccdEventErrorHandler;
    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;

    public CcdEventConsumer(ServiceBusConfiguration serviceBusConfiguration,
                            CcdEventProcessor ccdEventProcessor,
                            CcdEventErrorHandler ccdEventErrorHandler,
                            LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.ccdEventProcessor = ccdEventProcessor;
        this.ccdEventErrorHandler = ccdEventErrorHandler;
        this.featureFlagProvider = featureFlagProvider;
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

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1)
                .forEach(
                    message -> {

                        String incomingMessage = new String(message.getBody().toBytes());
                        try {
                            log.info("Received message with id '{}'", message.getMessageId());

                            if (featureFlagProvider.getBooleanValue(DLQ_DB_PROCESS,
                                                                     UserIdParser.getUserId(incomingMessage))) {
                                log.info("Feature flag '{}' evaluated to true. Message is not processed",
                                         DLQ_DB_PROCESS.getKey());
                            } else {
                                ccdEventProcessor.processMessage(incomingMessage);
                            }
                            receiver.complete(message);

                            log.info("Message with id '{}' handled successfully", message.getMessageId());
                        } catch (JsonProcessingException ex) {
                            ccdEventErrorHandler.handleJsonError(receiver, message, ex);
                        } catch (RestClientException ex) {
                            ccdEventErrorHandler.handleApplicationError(receiver, message, ex);
                        } catch (Exception ex) {
                            ccdEventErrorHandler.handleGenericError(receiver, message, ex);
                        }
                    });
        } catch (IllegalStateException ex) {
            log.info("Timeout: No messages received waiting for next session.");
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }
}

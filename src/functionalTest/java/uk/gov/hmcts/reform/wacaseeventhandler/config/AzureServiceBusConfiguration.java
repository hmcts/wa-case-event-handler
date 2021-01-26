package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.core.util.ClientOptions;
import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import javax.annotation.PreDestroy;

@Configuration
@ActiveProfiles(profiles = {"local", "functional"})
public class AzureServiceBusConfiguration {

    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;
    @Value("${azure.subscription}")
    private String subscription;

    @Autowired
    private ConsumerMessageProcessor messageProcessor;

    @Autowired
    private ConsumerMessageErrorProcessor errorProcessor;

    @Bean
    public ServiceBusSenderClient createConnection() {
        ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(host)
            .sender()
            .topicName(topic)
            .buildClient();

        return sender;
    }

/*
    @Bean
    public ServiceBusSessionReceiverAsyncClient serviceBusSessionReceiverConfig() {
        ServiceBusSessionReceiverAsyncClient sessionReceiver = new ServiceBusClientBuilder()
            .connectionString(host)
            .sessionReceiver()
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .topicName(topic)
            .subscriptionName(subscription)
            .buildAsyncClient();


        return sessionReceiver;
    }
*/


    @Bean
    public void serviceBusProcessorClient() {
        ServiceBusProcessorClient processor = new ServiceBusClientBuilder()
            .connectionString(host)
            .sessionProcessor()
            .topicName(topic)
            .subscriptionName(subscription)
            .processMessage(messageProcessor.receiveMessageWithSession())
            .processError(errorProcessor.errorHandler())
            .maxConcurrentSessions(500)
            //.maxConcurrentCalls(2)
            .buildProcessorClient();

        processor.start();
    }


}

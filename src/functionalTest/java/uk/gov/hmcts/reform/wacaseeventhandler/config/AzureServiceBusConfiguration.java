package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdEventMessageConsumer;

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
    private CcdEventMessageConsumer consumer;

    private ServiceBusProcessorClient serviceBusClient;

    @Bean
    public ServiceBusSenderClient createConnection() {
        ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(host)
            .sender()
            .topicName(topic)
            .buildClient();

        return sender;
    }

    @Bean
    public void serviceBusProcessorClient() {
        serviceBusClient = new ServiceBusClientBuilder()
            .connectionString(host)
            .sessionProcessor()
            .topicName(topic)
            .subscriptionName(subscription)
            .maxConcurrentSessions(11)
            .processMessage(consumer.consumeMessage())
            .processError(consumer.handleError())
            .buildProcessorClient();

        serviceBusClient.start();
    }

    @PreDestroy
    public void closeSender() {
        serviceBusClient.close();
    }
}

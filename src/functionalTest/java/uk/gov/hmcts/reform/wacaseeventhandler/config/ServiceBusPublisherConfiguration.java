package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
@ConfigurationProperties()
@ConditionalOnProperty("enableServiceBus")
public class ServiceBusPublisherConfiguration {

    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;

    private ServiceBusSenderClient publisher;

    @Bean("serviceBusSenderClient")
    public ServiceBusSenderClient createConnection() {
        publisher = new ServiceBusClientBuilder()
            .connectionString(host)
            .sender()
            .topicName(topic)
            .buildClient();

        return publisher;
    }

    @PreDestroy
    public void close() {
        if (publisher != null) {
            publisher.close();
        }
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
@ConditionalOnExpression("${azure.servicebus.enableASB:true} || ${azure.servicebus.enableASB-DLQ:true}")
public class CcdEventPublisherConfiguration {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;
    @Value("${azure.servicebus.topic-name}")
    private String topic;

    private ServiceBusSenderClient publisher;

    @Bean("serviceBusSenderClient")
    public ServiceBusSenderClient createConnection() {
        publisher = new ServiceBusClientBuilder()
            .connectionString(connectionString)
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

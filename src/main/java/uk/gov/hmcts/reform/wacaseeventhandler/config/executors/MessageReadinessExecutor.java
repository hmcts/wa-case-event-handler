package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.MessageReadinessConsumer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@ConditionalOnProperty("azure.servicebus.enableASB")
public class MessageReadinessExecutor {
    @Value("${scheduledExecutors.messageReadiness.pollIntervalSeconds}")
    private int pollInterval;

    @Value("${scheduledExecutors.messageReadiness.threadPoolSize}")
    private int threadPoolSize;

    @Autowired
    private MessageReadinessConsumer messageReadinessConsumer;

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void createMessageReadinessConsumer() {
        final ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(threadPoolSize);

        scheduledExecutorService.scheduleAtFixedRate(messageReadinessConsumer, 5, pollInterval, TimeUnit.SECONDS);
    }
}

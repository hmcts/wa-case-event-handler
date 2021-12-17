package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdCaseEventsDeadLetterQueueConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Configuration
@ConditionalOnProperty("azure.servicebus.enableASB")
public class CcdCaseEventsDeadLetterQueueExecutor {

    @Value("${azure.servicebus.threads}")
    private int concurrentSessions;

    @Autowired
    private CcdCaseEventsDeadLetterQueueConsumer serviceBusTask;

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void createDeadLetterQueueServiceBus() {
        final ExecutorService executorService = Executors.newFixedThreadPool(
            Integer.valueOf(concurrentSessions));

        IntStream.range(0, concurrentSessions).forEach(
            task -> executorService.execute(serviceBusTask));
    }

}

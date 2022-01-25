package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdCaseEventsConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Configuration
@ConditionalOnProperty("azure.servicebus.enableASB")
public class CcdCaseEventsExecutor {

    @Value("${azure.servicebus.threads}")
    private int concurrentSessions;

    @Autowired
    private CcdCaseEventsConsumer serviceBusTask;

    @Bean
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void createCcdCaseEventServiceBus() {
        final ExecutorService executorService = Executors.newFixedThreadPool(
            Integer.valueOf(concurrentSessions));

        IntStream.range(0, concurrentSessions).forEach(
            task -> executorService.execute(serviceBusTask));
    }

}

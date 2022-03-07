package uk.gov.hmcts.reform.wacaseeventhandler.config.executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@Profile("!functional & !local")
public class ExecutorServiceConfig {

    @Value("${azure.servicebus.threads}")
    private int concurrentSessions;

    @Bean("ccdCaseEventExecutorService")
    public ExecutorService createCcdCaseEventExecutorService() {
        return Executors.newFixedThreadPool(concurrentSessions);
    }

    @Bean("deadLetterQueueExecutorService")
    public ExecutorService createDeadLetterQueueExecutorService() {
        return Executors.newFixedThreadPool(concurrentSessions);
    }

    @Bean("ccdEventExecutorService")
    public ExecutorService createccdEventExecutorService() {
        return Executors.newFixedThreadPool(concurrentSessions);
    }

    @Bean("databaseMessageExecutorService")
    public ScheduledExecutorService createDatabaseMessageExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean("messageReadinessExecutorService")
    public ScheduledExecutorService createMessageReadinessExecutorService() {
        return Executors.newScheduledThreadPool(1);
    }
}

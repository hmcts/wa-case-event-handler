package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("db")
@TestMethodOrder(OrderAnnotation.class)
class CcdMessageProcessorExecutorTest {
    @SpyBean
    private DatabaseMessageConsumer databaseMessageConsumer;

    @TestConfiguration
    public static class LaunchDarklyConfig {

        @Bean
        @Primary
        public LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider() {
            LaunchDarklyFeatureFlagProvider flagProvider = mock(LaunchDarklyFeatureFlagProvider.class);
            when(flagProvider.getBooleanValue(any())).thenReturn(true, false);
            return flagProvider;
        }
    }

    @Test
    @Order(1)
    void should_create_database_message_consumer_when_launch_darkly_flag_enabled() throws InterruptedException {
        verify(databaseMessageConsumer, atLeast(1)).run();
    }

    @Test
    @Order(2)
    void should_not_create_database_message_consumer_when_launch_darkly_flag_disabled() {
        verify(databaseMessageConsumer, never()).run();
    }
}
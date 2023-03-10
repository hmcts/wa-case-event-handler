package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CaffeineConfiguration {

    @Value("${caffeine.calendar.timeout.duration}")
    private Integer calendarCacheDuration;

    @Value("#{T(java.util.concurrent.TimeUnit).of('${caffeine.calendar.timeout.unit}')}")
    private TimeUnit calendarCacheDurationUnit;

    @Bean
    public Ticker ticker() {
        return Ticker.systemTicker();
    }

    @Bean
    public Caffeine<Object, Object> calendarCaffeineConfig(Ticker ticker) {
        return Caffeine.newBuilder()
            .expireAfterWrite(calendarCacheDuration, calendarCacheDurationUnit)
            .ticker(ticker);
    }

    @Bean
    public CacheManager calendarCacheManager(Caffeine<Object, Object> calendarCaffeineConfig) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(calendarCaffeineConfig);
        caffeineCacheManager.setCacheNames(List.of("calendar_cache"));
        return caffeineCacheManager;
    }


}

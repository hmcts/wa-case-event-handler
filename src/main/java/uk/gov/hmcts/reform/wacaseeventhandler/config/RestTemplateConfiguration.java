package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zalando.logbook.httpclient5.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient5.LogbookHttpResponseInterceptor;

import java.util.function.Supplier;

@Configuration
public class RestTemplateConfiguration {

    private final LogbookHttpRequestInterceptor logbookHttpRequestInterceptor;
    private final LogbookHttpResponseInterceptor logbookHttpResponseInterceptor;

    public RestTemplateConfiguration(LogbookHttpRequestInterceptor logbookHttpRequestInterceptor,
                                     LogbookHttpResponseInterceptor logbookHttpResponseInterceptor) {
        this.logbookHttpRequestInterceptor = logbookHttpRequestInterceptor;
        this.logbookHttpResponseInterceptor = logbookHttpResponseInterceptor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.requestFactory(new RequestFactorySupplier()).build();
    }

    class RequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {
        @Override
        public ClientHttpRequestFactory get() {
            HttpClient client = HttpClientBuilder.create()
                .addRequestInterceptorFirst(logbookHttpRequestInterceptor)
                .addResponseInterceptorFirst(logbookHttpResponseInterceptor)
                .build();
            return new HttpComponentsClientHttpRequestFactory(client);
        }
    }

}

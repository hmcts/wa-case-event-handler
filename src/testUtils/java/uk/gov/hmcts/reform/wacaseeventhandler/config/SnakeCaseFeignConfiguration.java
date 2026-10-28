package uk.gov.hmcts.reform.wacaseeventhandler.config;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SnakeCaseFeignConfiguration {

    private final JsonMapper jsonMapper;

    @Autowired
    public SnakeCaseFeignConfiguration(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Bean
    @Primary
    public Encoder feignFormEncoder(ObjectProvider<FeignHttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public Decoder feignDecoder() {
        return new ResponseEntityDecoder(new SpringDecoder(provider(new JacksonJsonHttpMessageConverter(jsonMapper))));
    }

    @Bean
    public Encoder feignEncoder() {
        return new SpringEncoder(provider(new JacksonJsonHttpMessageConverter(jsonMapper)));
    }

    private static ObjectProvider<FeignHttpMessageConverters> provider(HttpMessageConverter<?> converter) {
        FeignHttpMessageConverters converters = new FeignHttpMessageConverters(unusedProvider(), unusedProvider()) {
            @Override
            public List<HttpMessageConverter<?>> getConverters() {
                return List.of(converter);
            }
        };
        return new ObjectProvider<>() {
            @Override
            public FeignHttpMessageConverters getObject() {
                return converters;
            }

            @Override
            public FeignHttpMessageConverters getObject(Object... args) {
                return converters;
            }

            @Override
            public FeignHttpMessageConverters getIfAvailable() {
                return converters;
            }

            @Override
            public FeignHttpMessageConverters getIfUnique() {
                return converters;
            }
        };
    }

    private static <T> ObjectProvider<T> unusedProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                throw new IllegalStateException("Feign converter provider is unused");
            }

            @Override
            public T getObject(Object... args) {
                return getObject();
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public T getIfUnique() {
                return null;
            }
        };
    }
}

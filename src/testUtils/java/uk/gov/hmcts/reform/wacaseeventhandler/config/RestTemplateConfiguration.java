package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestOperations restOperations(
        JacksonJsonHttpMessageConverter mappingJackson2HttpMessageConverter
    ) {
        return restTemplate(mappingJackson2HttpMessageConverter);
    }

    @Bean
    public RestTemplate restTemplate(
        JacksonJsonHttpMessageConverter mappingJackson2HttpMessageConverter
    ) {
        RestTemplate restTemplate = new RestTemplate();
        //Remove default
        restTemplate.getMessageConverters().removeIf(JacksonJsonHttpMessageConverter.class::isInstance);
        //Add autowired message converters as defined in JacksonConfiguration.java
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);

        return restTemplate;
    }

}

package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.CamundaProcess;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.CamundaProcessVariables;

import java.util.List;

@SuppressWarnings("PMD.UseObjectForClearerAPI")
@FeignClient(
    name = "camunda",
    url = "${targets.camunda}",
    configuration = CamundaClient.CamundaFeignConfiguration.class
)
public interface CamundaClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(value = "/process-instance",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    List<CamundaProcess> getProcessInstancesByVariables(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @RequestParam("variables") String variables,
        @RequestParam("activityIdIn") List<String> activityId
    );

    @GetMapping(value = "/process-instance/{key}/variables",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    CamundaProcessVariables getProcessInstanceVariables(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String processInstanceKey
    );

    @DeleteMapping(value = "/process-instance/{key}")
    void deleteProcessInstance(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String key
    );

    class CamundaFeignConfiguration {

        @Bean
        public Decoder feignDecoder() {
            return new JacksonDecoder(camundaObjectMapper());
        }

        @Bean
        public Encoder feignEncoder() {
            return new JacksonEncoder(camundaObjectMapper());
        }

        public ObjectMapper camundaObjectMapper() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
            objectMapper.registerModule(new Jdk8Module());
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper;
        }
    }
}


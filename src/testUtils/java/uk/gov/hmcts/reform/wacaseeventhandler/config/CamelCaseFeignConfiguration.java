package uk.gov.hmcts.reform.wacaseeventhandler.config;

//@Configuration
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CamelCaseFeignConfiguration {

    /*private final ObjectMapper objectMapper;

    @Autowired
    public CamelCaseFeignConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public Decoder feignDecoderCamelCase() {
        HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(camelCasedObjectMapper());
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }

    @Bean
    public Encoder feignEncoderCamelCase() {
        HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(camelCasedObjectMapper());
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);
        return new SpringEncoder(objectFactory);
    }

    public ObjectMapper camelCasedObjectMapper() {
        //Override naming strategy for this class only
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        return objectMapper;
    }*/
}

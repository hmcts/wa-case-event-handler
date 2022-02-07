package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wacaseeventhandler.config.DocumentManagementFiles;
import uk.gov.hmcts.reform.wacaseeventhandler.config.GivensBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.config.RestApiActions;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdamService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wacaseeventhandler.utils.Common;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {"local", "functional"})
public abstract class SpringBootFunctionalBaseTest {

    //todo: check here
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CAMUNDA_DATE_REQUEST_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS+0000";
    @Value("${targets.instance}") protected String testUrl;
    @Value("${targets.camunda}") public String camundaUrl;

    public ServiceBusSenderClient publisher;
    public String s2sToken;
    protected GivensBuilder given;
    protected Common common;
    protected RestApiActions camundaApiActions;

    @Autowired protected AuthorizationProvider authorizationProvider;
    @Autowired protected CoreCaseDataApi coreCaseDataApi;
    @Autowired protected DocumentManagementFiles documentManagementFiles;
    @Autowired protected IdamService idamService;
    @Autowired protected RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired private AuthTokenGenerator authTokenGenerator;
    @Autowired private ApplicationContext applicationContext;

    @Before
    public void setUp() throws IOException {
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (type, s) -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
                    objectMapper.registerModule(new Jdk8Module());
                    objectMapper.registerModule(new JavaTimeModule());
                    return objectMapper;
                }
            ));
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        s2sToken = authTokenGenerator.generate();
        if (applicationContext.containsBean("serviceBusSenderClient")) {
            publisher = (ServiceBusSenderClient) applicationContext.getBean("serviceBusSenderClient");
        }

        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();

        documentManagementFiles.prepare();

        given = new GivensBuilder(
            camundaApiActions,
            authorizationProvider,
            coreCaseDataApi,
            documentManagementFiles
        );

        common = new Common(
            given,
            camundaApiActions,
            authorizationProvider,
            idamService,
            roleAssignmentServiceApi
        );
    }

    public void waitSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

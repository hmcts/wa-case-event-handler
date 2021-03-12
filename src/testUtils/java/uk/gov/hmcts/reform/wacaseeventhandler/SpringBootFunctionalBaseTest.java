package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import io.restassured.RestAssured;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.concurrent.TimeUnit;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {"local", "functional"})
public abstract class SpringBootFunctionalBaseTest {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final int DEFAULT_POLLING_INTERVAL_MS = 500;
    public static final int DEFAULT_POLLING_TIMEOUT_SEC = 30;


    @Value("${targets.instance}")
    protected String testUrl;

    @Value("${targets.camunda}")
    public String camundaUrl;

    @Autowired
    public AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ApplicationContext applicationContext;

    public ServiceBusSenderClient publisher;

    public String s2sToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        s2sToken = authTokenGenerator.generate();
        if (applicationContext.containsBean("serviceBusSenderClient")) {
            publisher = (ServiceBusSenderClient) applicationContext.getBean("serviceBusSenderClient");
        }
    }

    public void waitSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

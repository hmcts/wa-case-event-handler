package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;

import static org.junit.Assert.assertNotNull;

public class ServiceTokenGeneratorConfigurationTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Test
    public void should_generate_a_token() {
        String token = authTokenGenerator.generate();
        assertNotNull(token);

    }
}

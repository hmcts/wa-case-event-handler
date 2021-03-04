package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFunctionalTestClient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LaunchDarklyFunctionalTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private LaunchDarklyFunctionalTestClient launchDarklyFunctionalTestClient;


    @Test
    public void should_hit_launch_darkly() {
        boolean launchDarklyFeature = launchDarklyFunctionalTestClient.getKey("tester");

        assertThat(launchDarklyFeature, is(false));
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class LaunchDarklyFunctionalTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private LaunchDarklyClient launchDarklyClient;

    @Test
    public void should_hit_launch_darkly_and_return_true() {
        boolean launchDarklyFeature = launchDarklyClient.getBooleanValueFromKey("tester");
        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_false() {
        boolean launchDarklyFeature = launchDarklyClient.getBooleanValueFromKey("non-existent");
        assertThat(launchDarklyFeature, is(false));
    }

    @Test
    public void should_hit_launch_darkly_with_azure_servicebus_and_return_off_string() {
        String launchDarklyFeature = launchDarklyClient.getStringValueFromKey("azure-servicebus");
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_with_message_servicebus_and_return_off_string() {
        String launchDarklyFeature = launchDarklyClient.getStringValueFromKey("message-servicebus");
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_with_amqp_and_return_off_string() {
        String launchDarklyFeature = launchDarklyClient.getStringValueFromKey("amqp");
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.AZURE_AMQP_LOGS;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.AZURE_MESSAGING_SERVICE_BUS_LOGS;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.AZURE_SERVICE_BUS_LOGS;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.NON_EXISTENT_KEY;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.TEST_KEY;

public class LaunchDarklyFeatureFlagProviderTest extends SpringBootFunctionalBaseTest {

    public static final String SOME_USER_ID = "some user id";

    @Autowired
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Test
    public void should_hit_launch_darkly_and_return_true() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(TEST_KEY, SOME_USER_ID);
        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_default_value_for_boolean() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(NON_EXISTENT_KEY, SOME_USER_ID);
        assertThat(launchDarklyFeature, is(false));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_default_value_for_string() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(NON_EXISTENT_KEY, SOME_USER_ID);
        assertThat(launchDarklyFeature, is(""));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_amqp_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_AMQP_LOGS, SOME_USER_ID);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_messaging_sb_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_MESSAGING_SERVICE_BUS_LOGS, SOME_USER_ID);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_sb_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_SERVICE_BUS_LOGS, SOME_USER_ID);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }
}

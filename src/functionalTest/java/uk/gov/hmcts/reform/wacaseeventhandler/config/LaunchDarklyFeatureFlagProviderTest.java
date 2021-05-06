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
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.TASK_INITIATION_FEATURE;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.TEST_KEY;

public class LaunchDarklyFeatureFlagProviderTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Test
    public void should_hit_launch_darkly_and_return_true() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(TEST_KEY, eventInformation.getUserId());
        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_default_value_for_boolean() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(NON_EXISTENT_KEY,
                                                                          eventInformation.getUserId()
        );
        assertThat(launchDarklyFeature, is(false));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_default_value_for_string() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(NON_EXISTENT_KEY);
        assertThat(launchDarklyFeature, is(""));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_amqp_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_AMQP_LOGS);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_messaging_sb_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_MESSAGING_SERVICE_BUS_LOGS);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_for_azure_sb_logs_value_and_return_either_on_or_off() {
        String launchDarklyFeature = featureFlagProvider.getStringValue(AZURE_SERVICE_BUS_LOGS);
        assertThat(launchDarklyFeature, either(equalTo("OFF")).or(equalTo("ON")));
    }

    @Test
    public void should_hit_launch_darkly_for_task_initiation_feature_and_return_either_true_or_false() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE,
                                                                          eventInformation.getUserId()
        );
        assertThat(launchDarklyFeature, either(equalTo(true)).or(equalTo(false)));
    }
}

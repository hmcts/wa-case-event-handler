package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchDarklyFeatureFlagProviderTest {

    @Mock
    private LDClientInterface ldClient;

    @InjectMocks
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private FeatureFlag featureFlag;
    private String launchDarklyVariableKey;

    @BeforeEach
    void setup() {
        launchDarklyVariableKey = "a-launch-darkly-key";

        featureFlag = mock(FeatureFlag.class);
        when(featureFlag.getKey()).thenReturn(launchDarklyVariableKey);
    }

    @Test
    void getBooleanValue_should_return_default_value_when_key_does_not_exist() {

        when(ldClient.boolVariation(
            eq(launchDarklyVariableKey),
            any(LDUser.class),
            eq(false))
        ).thenReturn(true);

        assertTrue(launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag));
    }

    @Test
    void getBooleanValue_should_return_value_when_key_exists() {
        when(ldClient.boolVariation(
            eq(launchDarklyVariableKey),
            any(LDUser.class),
            eq(false))
        ).thenReturn(false);

        assertFalse(launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag));
    }

    @Test
    void getStringValue_should_return_default_value_when_key_does_not_exist() {

        when(ldClient.stringVariation(
            eq(launchDarklyVariableKey),
            any(LDUser.class),
            eq(""))
        ).thenReturn("");

        String response = launchDarklyFeatureFlagProvider.getStringValue(featureFlag);
        assertEquals("", response);

    }

    @Test
    void getStringValue_should_return_value_when_key_exists() {

        when(ldClient.stringVariation(
            eq(launchDarklyVariableKey),
            any(LDUser.class),
            eq(""))
        ).thenReturn("aValue");

        String response = launchDarklyFeatureFlagProvider.getStringValue(featureFlag);
        assertEquals("aValue", response);
    }
}

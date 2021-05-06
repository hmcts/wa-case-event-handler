package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchDarklyFeatureFlagProviderTest {

    @Mock
    private LDClientInterface ldClient;

    @InjectMocks
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private FeatureFlag featureFlag;
    private String launchDarklySomeFlag;
    private LDUser expectedLDUser;

    @BeforeEach
    void setup() {
        launchDarklySomeFlag = "a-launch-darkly-flag";

        featureFlag = mock(FeatureFlag.class);

        expectedLDUser = new LDUser.Builder("wa-case-event-handler")
            .name("some user id")
            .firstName("Work Allocation")
            .lastName("Case Event Handler")
            .build();
    }

    @ParameterizedTest
    @CsvSource({
        "false, true, true",
        "false, false, false"
    })
    void getBooleanValue_return_expectedFlagValue(
        boolean defaultValue,
        boolean boolVariationReturn,
        boolean expectedFlagValue
    ) {
        when(featureFlag.getKey()).thenReturn(launchDarklySomeFlag);
        when(ldClient.boolVariation(eq(launchDarklySomeFlag), eq(expectedLDUser), eq(defaultValue)))
            .thenReturn(boolVariationReturn);

        assertThat(launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag, "some user id"))
            .isEqualTo(expectedFlagValue);
        verify(featureFlag, times(2)).getKey();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, some user id, featureFlag is null",
        "TASK_INITIATION_FEATURE, NULL, userId is null",
    }, nullValues = "NULL")
    void getBooleanValue_edge_case_scenarios(FeatureFlag featureFlag, String userId, String expectedMessage) {
        assertThatThrownBy(() -> launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag, userId))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({
        "'', '', ''",
        "'', some string variation value, some string variation value"
    })
    void getStringValue_return_expectedFlagValue(
        String defaultValue,
        String stringVariationReturn,
        String expectedFlagValue
    ) {
        when(featureFlag.getKey()).thenReturn(launchDarklySomeFlag);
        when(ldClient.stringVariation(eq(launchDarklySomeFlag), eq(expectedLDUser), eq(defaultValue)))
            .thenReturn(stringVariationReturn);

        assertThat(launchDarklyFeatureFlagProvider.getStringValue(featureFlag, "some user id"))
            .isEqualTo(expectedFlagValue);
        verify(featureFlag, times(2)).getKey();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, some user id, featureFlag is null",
        "TASK_INITIATION_FEATURE, NULL, userId is null",
    }, nullValues = "NULL")
    void getStringValue_edge_case_scenarios(FeatureFlag featureFlag, String userId, String expectedMessage) {
        assertThatThrownBy(() -> launchDarklyFeatureFlagProvider.getStringValue(featureFlag, userId))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

}

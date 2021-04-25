package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag;

@Slf4j
@Service
public class LaunchDarklyFeatureFlagProvider {
    private final LDClientInterface ldClient;
    private final LDUser launchDarklyUser = createLaunchDarklyUser();

    public LaunchDarklyFeatureFlagProvider(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    private LDUser createLaunchDarklyUser() {
        return new LDUser.Builder("wa-case-event-handler")
            .firstName("Work Allocation")
            .lastName("Case Event Handler")
            .build();
    }

    public boolean getBooleanValue(FeatureFlag featureFlag) {
        log.debug("Attempting to retrieve feature flag '{}' as Boolean", featureFlag.getKey());
        return ldClient.boolVariation(featureFlag.getKey(), launchDarklyUser, false);
    }

    public String getStringValue(FeatureFlag featureFlag) {
        log.debug("Attempting to retrieve feature flag '{}' as String", featureFlag.getKey());
        return ldClient.stringVariation(featureFlag.getKey(), launchDarklyUser, "");
    }
}

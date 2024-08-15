package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class LaunchDarklyFeatureFlagProvider {
    private final LDClientInterface ldClient;
    private static final String APPLICATION_NAME = "wa-case-event-handler";
    private static final String FIRST_NAME = "Work Allocation";
    private static final String LAST_NAME = "Case Event Handler";

    private static final String MSG_NULL_FEATURE_FLAG = "featureFlag is null";

    public LaunchDarklyFeatureFlagProvider(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    private LDUser createLaunchDarklyUser(String userId) {
        return new LDUser.Builder(APPLICATION_NAME)
            .name(userId)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .build();
    }

    private LDUser createLaunchDarklyUser() {
        return new LDUser.Builder(APPLICATION_NAME)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .build();
    }

    public boolean getBooleanValue(FeatureFlag featureFlag) {
        requireNonNull(featureFlag, MSG_NULL_FEATURE_FLAG);
        log.trace("Attempting to retrieve feature flag '{}' as Boolean", featureFlag.getKey());
        return ldClient.boolVariation(featureFlag.getKey(), createLaunchDarklyUser(), false);
    }

    public boolean getBooleanValue(FeatureFlag featureFlag, String userId) {
        requireNonNull(featureFlag, MSG_NULL_FEATURE_FLAG);
        requireNonNull(userId, "userId is null");
        log.trace("Attempting to retrieve feature flag '{}' as Boolean", featureFlag.getKey());
        return ldClient.boolVariation(featureFlag.getKey(), createLaunchDarklyUser(userId), false);
    }

    public String getStringValue(FeatureFlag featureFlag, String userId) {
        requireNonNull(featureFlag, MSG_NULL_FEATURE_FLAG);
        requireNonNull(userId, "userId is null");
        log.trace("Attempting to retrieve feature flag '{}' as String", featureFlag.getKey());
        return ldClient.stringVariation(featureFlag.getKey(), createLaunchDarklyUser(userId), "");
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler.config;


import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LaunchDarklyClient {

    private final LDClientInterface ldClient;

    @Autowired
    public LaunchDarklyClient(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getKey(String key) {

        LDUser ldUser =  new LDUser.Builder("wa-case-event-handler")
            .build();

        return ldClient.boolVariation(key, ldUser, false);
    }
}

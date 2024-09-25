package uk.gov.hmcts.reform.wacaseeventhandler.services;

import feign.FeignException;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.RoleCode;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAccount;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.IdamWebApi;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.idam.UserInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.AUTHORIZATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest.SERVICE_AUTHORIZATION;

@Slf4j
@Service
@Import({IdamWebApi.class,IdamServiceApi.class})
public class AuthorizationProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;
    @Autowired
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamServiceApi idamServiceApi;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Value("${idam.test.userCleanupEnabled:false}")
    private boolean testUserDeletionEnabled;

    public void deleteAccount(String username) {

        if (testUserDeletionEnabled) {
            //If error is thrown while deleting the user, it will be caught and logged
            try {
                log.info("Deleting test account '{}'", username);
                idamServiceApi.deleteTestUser(username);
            } catch (FeignException e) {
                log.error("Failed to delete test account '{}'", username, e);
            }
        } else {
            log.info("Test User deletion feature flag was not enabled, user '{}' was not deleted", username);
        }
    }

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    public TestAuthenticationCredentials getWaCaseworkerAAuthorizationOnly(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(
            new RoleCode("caseworker-wa-task-officer"),
            new RoleCode("payments"),
            new RoleCode("caseworker-wa")
        );
        TestAccount testAccount = generateIdamTestAccount(emailPrefix, requiredRoles);

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(testAccount),
            getServiceAuthorizationHeader()
        );


        return new TestAuthenticationCredentials(testAccount, authenticationHeaders);

    }

    public Header getAuthorizationOnly(TestAccount account) {
        return getAuthorization(account.getUsername(), account.getPassword());
    }

    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );

    }

    public Headers getServiceAuthorizationHeadersOnly() {
        return new Headers(getServiceAuthorizationHeader());
    }

    public TestAuthenticationCredentials getNewWaTribunalCaseworker(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        TestAccount caseworker = getIdamWaTribunalCaseworkerCredentials(emailPrefix);

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(caseworker),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(caseworker, authenticationHeaders);
    }

    private TestAccount getIdamWaTribunalCaseworkerCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(
            new RoleCode("caseworker-wa-task-officer"),
            new RoleCode("payments"),
            new RoleCode("caseworker-wa")
        );
        return generateIdamTestAccount(emailPrefix, requiredRoles);
    }

    private Header getAuthorization(String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            username,
            user -> "Bearer " + idamWebApi.token(body).getAccessToken()
        );

        return new Header(AUTHORIZATION, accessToken);
    }

    private MultiValueMap<String, String> createIdamRequest(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("redirect_uri", idamRedirectUrl);
        body.add("client_id", idamClientId);
        body.add("client_secret", idamClientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("scope", userScope);

        return body;
    }

    private TestAccount generateIdamTestAccount(String emailPrefix, List<RoleCode> requiredRoles) {
        String password = "London01";

        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("password", password);
        body.put("forename", "WAFTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);

        AtomicBoolean accountCreated = new AtomicBoolean(false);
        AtomicReference<String> email = new AtomicReference<>("");
        await().ignoreException(Exception.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(120, SECONDS)
            .until(() -> {
                try {
                    email.set(emailPrefix + UUID.randomUUID() + "@fake.hmcts.net");
                    log.info("Attempting to create a new test account {}", email);
                    body.put("email", email);
                    idamServiceApi.createTestUser(body);
                    accountCreated.set(true);
                } catch (FeignException e) {
                    log.error("Failed to create test account, retrying...", e);
                    accountCreated.set(false);
                }
                return accountCreated.get();
            });

        log.info("Test account created successfully");
        return new TestAccount(email.get(), password);
    }
}

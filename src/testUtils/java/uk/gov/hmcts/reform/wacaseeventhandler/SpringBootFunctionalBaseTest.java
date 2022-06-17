package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttribute;
import uk.gov.hmcts.reform.wacaseeventhandler.config.DocumentManagementFiles;
import uk.gov.hmcts.reform.wacaseeventhandler.config.GivensBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.config.RestApiActions;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdamService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wacaseeventhandler.utils.Common;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TaskAttributeDefinition.TASK_TYPE;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {"local", "functional"})
@Slf4j
public abstract class SpringBootFunctionalBaseTest {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CAMUNDA_DATE_REQUEST_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS+0000";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    protected static final String TASK_ENDPOINT = "/task/{task-id}";

    @Value("${targets.instance}") protected String testUrl;
    @Value("${wa-task-management-api.url}") protected String taskManagementUrl;
    @Value("${targets.camunda}") public String camundaUrl;

    public ServiceBusSenderClient publisher;
    public String s2sToken;
    protected GivensBuilder given;
    protected Common common;
    protected RestApiActions camundaApiActions;
    protected RestApiActions restApiActions;

    @Autowired protected AuthorizationProvider authorizationProvider;
    @Autowired protected CoreCaseDataApi coreCaseDataApi;
    @Autowired protected DocumentManagementFiles documentManagementFiles;
    @Autowired protected IdamService idamService;
    @Autowired protected RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired private AuthTokenGenerator authTokenGenerator;
    @Autowired private ApplicationContext applicationContext;
    @Autowired protected IdempotencyKeyGenerator idempotencyKeyGenerator;

    protected List<String> caseIds;

    @Before
    public void setUp() throws IOException {
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (type, s) -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
                    objectMapper.registerModule(new Jdk8Module());
                    objectMapper.registerModule(new JavaTimeModule());
                    return objectMapper;
                }
            ));
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        s2sToken = authTokenGenerator.generate();
        if (applicationContext.containsBean("serviceBusSenderClient")) {
            publisher = (ServiceBusSenderClient) applicationContext.getBean("serviceBusSenderClient");
        }

        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        restApiActions = new RestApiActions(taskManagementUrl, SNAKE_CASE).setUp();

        documentManagementFiles.prepare();

        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            coreCaseDataApi,
            documentManagementFiles
        );

        common = new Common(
            given,
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            idamService,
            roleAssignmentServiceApi
        );

        caseIds = new ArrayList<>();
    }

    public void waitSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void initiateTask(Headers authenticationHeaders, String caseId, String taskId,
                                String taskType, String taskName, String taskTitle) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, taskName),
            new TaskAttribute(TASK_TITLE, taskTitle),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_ENDPOINT,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(taskId))
            .body("case_id", equalTo(caseId));
    }

    public String getCaseId() {
        return getCaseIdForJurisdictionAndCaseType("IA", "Asylum");
    }

    public String getCaseIdForJurisdictionAndCaseType(String jurisdictionId, String caseType) {
        TestVariables taskVariables = common.createCase(jurisdictionId, caseType);
        requireNonNull(taskVariables, "taskVariables is null");
        requireNonNull(taskVariables.getCaseId(), "case id is null");
        caseIds.add(taskVariables.getCaseId());
        return taskVariables.getCaseId();
    }

    protected Response findTasksByCaseId(
        String caseId, int expectedTaskAmount
    ) {

        log.info("Finding task for caseId = {}", caseId);
        AtomicReference<Response> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1000, MILLISECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = given()
                        .relaxedHTTPSValidation()
                        .header(SERVICE_AUTHORIZATION, s2sToken)
                        .contentType(APPLICATION_JSON_VALUE)
                        .baseUri(camundaUrl)
                        .basePath("/task")
                        .param("processVariables", "caseId_eq_" + caseId)
                        .when()
                        .get();

                    result
                        .then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .body("size()", is(expectedTaskAmount));

                    response.set(result);
                    return true;
                });

        return response.get();
    }

    protected Response findTaskDetailsForGivenTaskId(String taskId) {
        log.info("Attempting to retrieve task details with taskId = {}", taskId);

        return given()
            .header(SERVICE_AUTHORIZATION, s2sToken)
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task/" + taskId + "/variables")
            .when()
            .get();
    }
}

package uk.gov.hmcts.reform.wacaseeventhandler;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.config.CcdRetryableClient;
import uk.gov.hmcts.reform.wacaseeventhandler.config.GivensBuilder;
import uk.gov.hmcts.reform.wacaseeventhandler.config.RestApiActions;
import uk.gov.hmcts.reform.wacaseeventhandler.entities.TestVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdamService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.IdempotencyKeyGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.services.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wacaseeventhandler.utils.Common;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.UPPER_CAMEL_CASE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wacaseeventhandler.clients.request.InitiateTaskOperation.INITIATION;

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
    private static final String TASK_INITIATION_ENDPOINT = "task/{task-id}/initiation";

    @Value("${targets.instance}")
    protected String testUrl;
    @Value("${targets.taskapi}")
    protected String taskManagementUrl;
    @Value("${targets.camunda}")
    public String camundaUrl;

    public ServiceBusSenderClient publisher;
    public String s2sToken;
    protected GivensBuilder given;
    protected Common common;
    protected RestApiActions camundaApiActions;
    protected RestApiActions restApiActions;

    @Autowired
    protected AuthorizationProvider authorizationProvider;
    @Autowired
    protected CcdRetryableClient ccdRetryableClient;
    @Autowired
    protected IdamService idamService;
    @Autowired
    protected RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired
    private AuthTokenGenerator authTokenGenerator;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    protected IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    protected List<String> caseIds;

    public static final String WA_TASK_INITIATION_WA_ASYLUM = "wa-task-initiation-wa-wacasetype";
    public static final String PRIVATE_LAW_TASK_INITIATION_WA_ASYLUM = "wa-task-initiation-privatelaw-prlapps";

    public static final String TENANT_ID_WA = "wa";
    public static final String PRIVATE_LAW_TENANT_ID = "wa";

  @Before
  public void setUp() throws IOException {
    RestAssured.config = RestAssuredConfig.config()
      .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
        (type, s) -> {
          ObjectMapper objectMapper = new ObjectMapper();
          objectMapper.setPropertyNamingStrategy(UPPER_CAMEL_CASE);
          objectMapper.registerModule(new Jdk8Module());
          objectMapper.registerModule(new JavaTimeModule());
          return objectMapper;
        }
      ));
    RestAssured.baseURI = testUrl;
    RestAssured.useRelaxedHTTPSValidation();
    s2sToken = authTokenGenerator.generate();

    if (s2sToken == null) {
      log.error("s2sToken has not been set correctly. API tests will fail");
    }

    if (applicationContext.containsBean("serviceBusSenderClient")) {
      publisher = (ServiceBusSenderClient) applicationContext.getBean("serviceBusSenderClient");
    }

    camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
    restApiActions = new RestApiActions(taskManagementUrl, SNAKE_CASE).setUp();

    given = new GivensBuilder(
      camundaApiActions,
      authorizationProvider,
      ccdRetryableClient
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

    Map<String, Object> taskAttributes = new HashMap<>();
    taskAttributes.put("taskType", taskType);
    taskAttributes.put("name", taskName);
    taskAttributes.put("title", taskTitle);
    taskAttributes.put("caseId", caseId);
    taskAttributes.put("created", formattedCreatedDate);
    taskAttributes.put("dueDate", formattedDueDate);

    InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(INITIATION, taskAttributes);

    Response result = restApiActions.post(
      TASK_INITIATION_ENDPOINT,
      taskId,
      initiateTaskRequest,
      authenticationHeaders
    );

    assertResponse(result, caseId, taskId);
  }

  public String getWaCaseId() {
    TestVariables taskVariables = common.createWaCase();
    requireNonNull(taskVariables, "taskVariables is null");
    requireNonNull(taskVariables.getCaseId(), "case id is null");
    caseIds.add(taskVariables.getCaseId());
    return taskVariables.getCaseId();

  }

  protected Response findTasksByCaseId(String caseId, int expectedTaskAmount) {

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

  private void assertResponse(Response response, String caseId, String taskId) {
    response.prettyPrint();

    int statusCode = response.getStatusCode();
    switch (statusCode) {
      case 503:
        log.info("Initiation failed due to Database Conflict Error, so handling gracefully, {}", statusCode);

        response.then().assertThat()
          .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
          .contentType(APPLICATION_PROBLEM_JSON_VALUE)
          .body("type", equalTo(
            "https://github.com/hmcts/wa-task-management-api/problem/database-conflict"))
          .body("title", equalTo("Database Conflict Error"))
          .body("status", equalTo(503))
          .body("detail", equalTo(
            "Database Conflict Error: The action could not be completed because "
              + "there was a conflict in the database."));
        break;
      case 201:
        log.info("task Initiation got successfully with status, {}", statusCode);
        response.then().assertThat()
          .statusCode(HttpStatus.CREATED.value())
          .and()
          .contentType(APPLICATION_JSON_VALUE)
          .body("task_id", equalTo(taskId))
          .body("case_id", equalTo(caseId));
        break;
      default:
        log.info("task Initiation failed with status, {}", statusCode);
        throw new RuntimeException("Invalid status received for task initiation " + statusCode);
    }
  }
}
